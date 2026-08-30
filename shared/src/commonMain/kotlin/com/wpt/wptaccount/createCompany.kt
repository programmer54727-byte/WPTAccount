package com.wpt.wptaccount

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCompanyForm(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit
) {
    // General Information
    var companyName by remember { mutableStateOf("") }
    var mailingName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    // Contact Details
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var fax by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("") }
    
    // Financial Details
    var finYearBeginning by remember { mutableStateOf("01/04/2024") }
    var booksBeginning by remember { mutableStateOf("01/04/2024") }
    var baseCurrencySymbol by remember { mutableStateOf("₹") }
    var formalName by remember { mutableStateOf("INR") }
    
    // Security
    var tallyVaultEnabled by remember { mutableStateOf("No") }
    var controlAccessEnabled by remember { mutableStateOf("No") }

    // GST/HSN Defaults
    var gstApplicability by remember { mutableStateOf("Applicable") }
    var hsnNumber by remember { mutableStateOf("") }
    var hsnDescription by remember { mutableStateOf("") }
    var gstRate by remember { mutableStateOf("0") }
    var typeOfSupply by remember { mutableStateOf("Goods") }

    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    fun performCreateCompany() {
        if (companyName.isBlank()) {
            saveError = "Company Name is required"
            return
        }
        
        scope.launch {
            isSaving = true
            saveError = null
            println("Starting company creation for: $companyName")
            try {
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    val company = Company(
                        company_name = companyName,
                        mailing_name = mailingName,
                        address = address,
                        state = state,
                        country = country,
                        pincode = pincode,
                        telephone = telephone,
                        mobile = mobile,
                        fax = fax,
                        email = email,
                        website = website,
                        financial_year_beginning = finYearBeginning.toDbDate(),
                        books_beginning = booksBeginning.toDbDate(),
                        tally_vault_password_enabled = tallyVaultEnabled,
                        control_user_access_enabled = controlAccessEnabled,
                        base_currency_symbol = baseCurrencySymbol,
                        formal_name = formalName,
                        gst_applicability = gstApplicability,
                        hsn_sac_number = hsnNumber,
                        hsn_description = hsnDescription,
                        gst_rate = gstRate.toDoubleOrNull() ?: 0.0,
                        type_of_supply = typeOfSupply,
                        owner_id = user.id
                    )
                    
                    println("Inserting company into Supabase...")
                    val insertedCompany = supabase.from("companies").insert(company) {
                        select()
                    }.decodeSingle<Company>()
                    
                    val companyId = insertedCompany.id!!
                    initializeCompanySetup(companyId)
                    
                    println("Initial setup complete!")
                    onSuccess()
                } else {
                    saveError = "User not logged in"
                    println("Error: User not logged in")
                }
            } catch (e: Exception) {
                println("Create company error: ${e.message}")
                saveError = "Failed to create company. Please try again."
                e.printStackTrace()
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create New Company") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 800.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (saveError != null) {
                    Text(
                        text = saveError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                SectionHeader("General Information")
                OutlinedTextField(
                    value = companyName, 
                    onValueChange = { companyName = it }, 
                    label = { Text("Company Name *") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = mailingName, 
                    onValueChange = { mailingName = it }, 
                    label = { Text("Mailing Name") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = address, 
                    onValueChange = { address = it }, 
                    label = { Text("Address") }, 
                    modifier = Modifier.fillMaxWidth(), 
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )

                SectionHeader("Contact Details")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = state, 
                        onValueChange = { state = it }, 
                        label = { Text("State") }, 
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) })
                    )
                    OutlinedTextField(
                        value = country, 
                        onValueChange = { country = it }, 
                        label = { Text("Country") }, 
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                    )
                }
                OutlinedTextField(
                    value = pincode, 
                    onValueChange = { pincode = it }, 
                    label = { Text("Pincode") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = telephone, 
                    onValueChange = { telephone = it }, 
                    label = { Text("Telephone") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = mobile, 
                    onValueChange = { mobile = it }, 
                    label = { Text("Mobile") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = fax, 
                    onValueChange = { fax = it }, 
                    label = { Text("Fax") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = email, 
                    onValueChange = { email = it }, 
                    label = { Text("Email") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = website, 
                    onValueChange = { website = it }, 
                    label = { Text("Website") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )

                SectionHeader("Financial Details")
                OutlinedTextField(
                    value = finYearBeginning, 
                    onValueChange = { finYearBeginning = it }, 
                    label = { Text("Financial Year Beginning (YYYY-MM-DD)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                OutlinedTextField(
                    value = booksBeginning, 
                    onValueChange = { booksBeginning = it }, 
                    label = { Text("Books Beginning (YYYY-MM-DD)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) })
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = baseCurrencySymbol, 
                        onValueChange = { baseCurrencySymbol = it }, 
                        label = { Text("Currency Symbol") }, 
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) })
                    )
                    OutlinedTextField(
                        value = formalName, 
                        onValueChange = { formalName = it }, 
                        label = { Text("Formal Name") }, 
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { 
                            focusManager.clearFocus()
                            performCreateCompany()
                        })
                    )
                }

                SectionHeader("Security Control")
                SelectionRow("Tally Vault Password", tallyVaultEnabled) { tallyVaultEnabled = it }
                SelectionRow("Control User Access", controlAccessEnabled) { controlAccessEnabled = it }

                SectionHeader("GST/HSN Statutory Defaults")
                SelectionRow("GST Applicability", gstApplicability) { gstApplicability = it }
                if (gstApplicability == "Applicable") {
                    OutlinedTextField(
                        value = hsnNumber,
                        onValueChange = { hsnNumber = it },
                        label = { Text("HSN/SAC Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = hsnDescription,
                        onValueChange = { hsnDescription = it },
                        label = { Text("HSN Description") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = gstRate,
                        onValueChange = { gstRate = it },
                        label = { Text("Default GST Rate (%)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    
                    Text("Type of Supply", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = typeOfSupply == "Goods", onClick = { typeOfSupply = "Goods" })
                        Text("Goods", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(8.dp))
                        RadioButton(selected = typeOfSupply == "Services", onClick = { typeOfSupply = "Services" })
                        Text("Services", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Button(
                    onClick = { performCreateCompany() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    } else {
                        Text("Create Company")
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
fun SelectionRow(label: String, selected: String, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected == "Yes", onClick = { onSelect("Yes") })
            Text("Yes", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(8.dp))
            RadioButton(selected = selected == "No", onClick = { onSelect("No") })
            Text("No", style = MaterialTheme.typography.bodySmall)
        }
    }
}
