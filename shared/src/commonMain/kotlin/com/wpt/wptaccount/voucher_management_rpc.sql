-- 1. Create the atomic save function for vouchers
CREATE OR REPLACE FUNCTION public.save_voucher_v3(
    p_voucher_id UUID,
    p_company_id UUID,
    p_voucher_type TEXT,
    p_voucher_number TEXT,
    p_invoice_no TEXT,
    p_invoice_date DATE,
    p_party_ledger_id UUID,
    p_date DATE,
    p_narration TEXT,
    p_total_amount DECIMAL,
    p_entries JSONB,       -- List of {ledger_id, amount, entry_type}
    p_stock_items JSONB,   -- List of {stock_item_id, quantity, rate, amount, hsn_code, gst_rate}
    p_references JSONB     -- List of {reference_type, reference_no, amount, ledger_id}
)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_voucher_id UUID;
    v_item RECORD;
    v_entry RECORD;
    v_ref RECORD;
    v_nature TEXT;
    v_adjustment DECIMAL;
BEGIN
    -- 1. UNDO EXISTING EFFECTS (If editing)
    IF p_voucher_id IS NOT NULL THEN
        -- Reverse Stock Items
        FOR v_item IN SELECT * FROM public.voucher_stock_items WHERE voucher_id = p_voucher_id LOOP
            IF p_voucher_type = 'Purchase' THEN
                UPDATE public.stock_items SET current_quantity = current_quantity - v_item.quantity WHERE id = v_item.stock_item_id;
            ELSE
                UPDATE public.stock_items SET current_quantity = current_quantity + v_item.quantity WHERE id = v_item.stock_item_id;
            END IF;
        END LOOP;

        -- Reverse Ledger Balances
        FOR v_entry IN
            SELECT ve.*, g.nature
            FROM public.voucher_entries ve
            JOIN public.ledgers l ON ve.ledger_id = l.id
            JOIN public.groups g ON l.group_id = g.id
            WHERE ve.voucher_id = p_voucher_id
        LOOP
            IF v_entry.nature IN ('Asset', 'Expense') THEN
                v_adjustment := CASE WHEN v_entry.entry_type = 'Debit' THEN -v_entry.amount ELSE v_entry.amount END;
            ELSE
                v_adjustment := CASE WHEN v_entry.entry_type = 'Debit' THEN v_entry.amount ELSE -v_entry.amount END;
            END IF;
            UPDATE public.ledgers SET current_balance = current_balance + v_adjustment WHERE id = v_entry.ledger_id;
        END LOOP;

        DELETE FROM public.voucher_entries WHERE voucher_id = p_voucher_id;
        DELETE FROM public.voucher_stock_items WHERE voucher_id = p_voucher_id;
        DELETE FROM public.voucher_references WHERE voucher_id = p_voucher_id;

        v_voucher_id := p_voucher_id;

        UPDATE public.vouchers SET
            company_id = p_company_id,
            voucher_type = p_voucher_type,
            voucher_number = p_voucher_number,
            invoice_no = p_invoice_no,
            invoice_date = p_invoice_date,
            party_ledger_id = p_party_ledger_id,
            date = p_date,
            narration = p_narration,
            total_amount = p_total_amount
        WHERE id = v_voucher_id;
    ELSE
        INSERT INTO public.vouchers (
            company_id, voucher_type, voucher_number, invoice_no, invoice_date,
            party_ledger_id, date, narration, total_amount
        ) VALUES (
            p_company_id, p_voucher_type, p_voucher_number, p_invoice_no, p_invoice_date,
            p_party_ledger_id, p_date, p_narration, p_total_amount
        ) RETURNING id INTO v_voucher_id;
    END IF;

    -- 2. APPLY NEW EFFECTS

    -- Insert Stock Items & Update Quantity
    FOR v_item IN SELECT * FROM jsonb_to_recordset(p_stock_items) AS x(stock_item_id UUID, quantity DECIMAL, rate DECIMAL, amount DECIMAL, hsn_code TEXT, gst_rate DECIMAL) LOOP
        INSERT INTO public.voucher_stock_items (voucher_id, stock_item_id, quantity, rate, amount, hsn_code, gst_rate)
        VALUES (v_voucher_id, v_item.stock_item_id, v_item.quantity, v_item.rate, v_item.amount, v_item.hsn_code, v_item.gst_rate);

        IF p_voucher_type = 'Purchase' THEN
            UPDATE public.stock_items SET current_quantity = current_quantity + v_item.quantity WHERE id = v_item.stock_item_id;
        ELSE
            UPDATE public.stock_items SET current_quantity = current_quantity - v_item.quantity WHERE id = v_item.stock_item_id;
        END IF;
    END LOOP;

    -- Insert Entries & Update Ledger Balance
    FOR v_entry IN SELECT * FROM jsonb_to_recordset(p_entries) AS x(ledger_id UUID, amount DECIMAL, entry_type TEXT) LOOP
        INSERT INTO public.voucher_entries (voucher_id, ledger_id, amount, entry_type)
        VALUES (v_voucher_id, v_entry.ledger_id, v_entry.amount, v_entry.entry_type);

        SELECT g.nature INTO v_nature
        FROM public.ledgers l
        JOIN public.groups g ON l.group_id = g.id
        WHERE l.id = v_entry.ledger_id;

        IF v_nature IN ('Asset', 'Expense') THEN
            v_adjustment := CASE WHEN v_entry.entry_type = 'Debit' THEN v_entry.amount ELSE -v_entry.amount END;
        ELSE
            v_adjustment := CASE WHEN v_entry.entry_type = 'Debit' THEN -v_entry.amount ELSE v_entry.amount END;
        END IF;

        UPDATE public.ledgers SET current_balance = current_balance + v_adjustment WHERE id = v_entry.ledger_id;
    END LOOP;

    -- Insert References
    FOR v_ref IN SELECT * FROM jsonb_to_recordset(p_references) AS x(reference_type TEXT, reference_no TEXT, amount DECIMAL, ledger_id UUID) LOOP
        INSERT INTO public.voucher_references (voucher_id, ledger_id, reference_type, reference_no, amount)
        VALUES (v_voucher_id, v_ref.ledger_id, v_ref.reference_type, v_ref.reference_no, v_ref.amount);
    END LOOP;

    RETURN v_voucher_id;
END;
$$;

-- 2. Create the atomic delete function
CREATE OR REPLACE FUNCTION public.delete_voucher_v3(
    p_voucher_id UUID
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_item RECORD;
    v_entry RECORD;
    v_voucher_type TEXT;
    v_adjustment DECIMAL;
BEGIN
    -- Get voucher type for stock undo
    SELECT voucher_type INTO v_voucher_type FROM public.vouchers WHERE id = p_voucher_id;

    -- Reverse Stock Items
    FOR v_item IN SELECT * FROM public.voucher_stock_items WHERE voucher_id = p_voucher_id LOOP
        IF v_voucher_type = 'Purchase' THEN
            UPDATE public.stock_items SET current_quantity = current_quantity - v_item.quantity WHERE id = v_item.stock_item_id;
        ELSE
            UPDATE public.stock_items SET current_quantity = current_quantity + v_item.quantity WHERE id = v_item.stock_item_id;
        END IF;
    END LOOP;

    -- Reverse Ledger Balances
    FOR v_entry IN
        SELECT ve.*, g.nature
        FROM public.voucher_entries ve
        JOIN public.ledgers l ON ve.ledger_id = l.id
        JOIN public.groups g ON l.group_id = g.id
        WHERE ve.voucher_id = p_voucher_id
    LOOP
        IF v_entry.nature IN ('Asset', 'Expense') THEN
            v_adjustment := CASE WHEN v_entry.entry_type = 'Debit' THEN -v_entry.amount ELSE v_entry.amount END;
        ELSE
            v_adjustment := CASE WHEN v_entry.entry_type = 'Debit' THEN v_entry.amount ELSE -v_entry.amount END;
        END IF;
        UPDATE public.ledgers SET current_balance = current_balance + v_adjustment WHERE id = v_entry.ledger_id;
    END LOOP;

    -- Delete the voucher (Cascade will handle related records if FKs are set,
    -- but we can be explicit if needed, though Cascade is safer in Postgres)
    DELETE FROM public.vouchers WHERE id = p_voucher_id;
END;
$$;
