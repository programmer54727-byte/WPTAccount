package com.wpt.wptaccount

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCompanyForm(
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    initialCompany: Company? = null
) {
    // General Information
    var companyName by rememberSaveable { mutableStateOf(initialCompany?.company_name ?: "") }
    var mailingName by rememberSaveable { mutableStateOf(initialCompany?.mailing_name ?: "") }
    var address by rememberSaveable { mutableStateOf(initialCompany?.address ?: "") }
    
    // Contact Details
    var state by rememberSaveable { mutableStateOf(initialCompany?.state ?: "") }
    var country by rememberSaveable { mutableStateOf(initialCompany?.country ?: "") }
    var pincode by rememberSaveable { mutableStateOf(initialCompany?.pincode ?: "") }
    var telephone by rememberSaveable { mutableStateOf(initialCompany?.telephone ?: "") }
    var mobile by rememberSaveable { mutableStateOf(initialCompany?.mobile ?: "") }
    var fax by rememberSaveable { mutableStateOf(initialCompany?.fax ?: "") }
    var email by rememberSaveable { mutableStateOf(initialCompany?.email ?: "") }
    var website by rememberSaveable { mutableStateOf(initialCompany?.website ?: "") }
    
    // Financial Details
    var finYearBeginning by rememberSaveable { mutableStateOf(initialCompany?.financial_year_beginning?.toDisplayDate() ?: "01/04/2024") }
    var booksBeginning by rememberSaveable { mutableStateOf(initialCompany?.books_beginning?.toDisplayDate() ?: "01/04/2024") }
    var baseCurrencySymbol by rememberSaveable { mutableStateOf(initialCompany?.base_currency_symbol ?: "₹") }
    var formalName by rememberSaveable { mutableStateOf(initialCompany?.formal_name ?: "INR") }
    
    // Security
    var tallyVaultEnabled by rememberSaveable { mutableStateOf(initialCompany?.tally_vault_password_enabled ?: "No") }
    var controlAccessEnabled by rememberSaveable { mutableStateOf(initialCompany?.control_user_access_enabled ?: "No") }

    // GST/HSN Defaults
    var gstApplicability by rememberSaveable { mutableStateOf(initialCompany?.gst_applicability ?: "Applicable") }
    var hsnNumber by rememberSaveable { mutableStateOf(initialCompany?.hsn_sac_number ?: "") }
    var hsnDescription by rememberSaveable { mutableStateOf(initialCompany?.hsn_description ?: "") }
    var gstRate by rememberSaveable { mutableStateOf(initialCompany?.gst_rate?.toString() ?: "0") }
    var typeOfSupply by rememberSaveable { mutableStateOf(initialCompany?.type_of_supply ?: "Goods") }

    var isSaving by rememberSaveable { mutableStateOf(false) }
    var saveError by rememberSaveable { mutableStateOf<String?>(null) }
    
    var countries by remember { mutableStateOf<List<CountryData>>(emptyList()) }
    LaunchedEffect(Unit) {
        countries = CountryRepository.getCountries()
    }
    
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    fun performSaveCompany() {
        if (companyName.isBlank()) {
            saveError = "Company Name is required"
            return
        }
        
        scope.launch {
            isSaving = true
            saveError = null
            println("Starting company save for: $companyName")
            try {
                val user = supabase.auth.currentUserOrNull()
                if (user != null) {
                    val company = Company(
                        id = initialCompany?.id,
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
                        owner_id = initialCompany?.owner_id ?: user.id
                    )
                    
                    if (initialCompany != null) {
                        println("Updating company in Supabase...")
                        supabase.from("companies").update(company) {
                            filter { eq("id", initialCompany.id!!) }
                        }
                    } else {
                        println("Inserting company into Supabase...")
                        val insertedCompany = supabase.from("companies").insert(company) {
                            select()
                        }.decodeSingle<Company>()
                        
                        val companyId = insertedCompany.id!!
                        initializeCompanySetup(companyId)
                    }
                    
                    println("Save complete!")
                    onSuccess()
                } else {
                    saveError = "User not logged in"
                    println("Error: User not logged in")
                }
            } catch (e: Exception) {
                println("Save company error: ${e.message}")
                saveError = "Failed to save company. Please try again."
                e.printStackTrace()
            } finally {
                isSaving = false
            }
        }
    }

    Scaffold(
        containerColor = WptColors.AppSurface,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = { 
                    Text(
                        text = if (initialCompany != null) "Edit Company" else "Create New Company",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = WptColors.PrimaryText
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = WptColors.PrimaryAccent)
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                if (saveError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFDE8E8))
                    ) {
                        Text(
                            text = saveError!!,
                            color = WptColors.Error,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // Section 1: General Information
                FormSectionCard(title = "General Information") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = companyName, 
                            onValueChange = { companyName = it }, 
                            label = { Text("Company Name *") }, 
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                        OutlinedTextField(
                            value = mailingName, 
                            onValueChange = { mailingName = it }, 
                            label = { Text("Mailing Name") }, 
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                        OutlinedTextField(
                            value = address, 
                            onValueChange = { address = it }, 
                            label = { Text("Address") }, 
                            modifier = Modifier.fillMaxWidth(), 
                            minLines = 2,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                    }
                }

                // Section 2: Contact Details
                FormSectionCard(title = "Contact Details") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SearchableDropdown(
                            label = "Country",
                            options = countries.map { it.country },
                            selected = country,
                            modifier = Modifier.fillMaxWidth(),
                            labelWidth = 100.dp
                        ) {
                            country = it
                            val selectedCountry = countries.find { c -> c.country == it }
                            if (selectedCountry != null) {
                                val allRegions = selectedCountry.getAllRegions()
                                if (allRegions.none { r -> r.name == state }) {
                                    state = ""
                                }
                            }
                        }

                        val currentCountryStates = countries.find { it.country == country }?.getAllRegions()?.map { it.name } ?: emptyList()
                        SearchableDropdown(
                            label = "State",
                            options = currentCountryStates,
                            selected = state,
                            modifier = Modifier.fillMaxWidth(),
                            labelWidth = 100.dp
                        ) {
                            state = it
                        }
                        
                        OutlinedTextField(
                            value = pincode, 
                            onValueChange = { pincode = it }, 
                            label = { Text("Pincode") }, 
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                        OutlinedTextField(
                            value = telephone, 
                            onValueChange = { telephone = it }, 
                            label = { Text("Telephone") }, 
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                        OutlinedTextField(
                            value = mobile, 
                            onValueChange = { mobile = it }, 
                            label = { Text("Mobile") }, 
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                        OutlinedTextField(
                            value = fax, 
                            onValueChange = { fax = it }, 
                            label = { Text("Fax") }, 
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                        OutlinedTextField(
                            value = email, 
                            onValueChange = { email = it }, 
                            label = { Text("Email") }, 
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                        OutlinedTextField(
                            value = website, 
                            onValueChange = { website = it }, 
                            label = { Text("Website") }, 
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                    }
                }

                // Section 3: Financial Details
                FormSectionCard(title = "Financial Details") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = finYearBeginning, 
                            onValueChange = { finYearBeginning = it }, 
                            label = { Text("Financial Year Beginning (DD/MM/YYYY)") }, 
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { 
                                    if (!it.isFocused && finYearBeginning.isNotEmpty()) finYearBeginning = finYearBeginning.formatSmartDate()
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                        OutlinedTextField(
                            value = booksBeginning, 
                            onValueChange = { booksBeginning = it }, 
                            label = { Text("Books Beginning (DD/MM/YYYY)") }, 
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { 
                                    if (!it.isFocused && booksBeginning.isNotEmpty()) booksBeginning = booksBeginning.formatSmartDate()
                                },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = baseCurrencySymbol, 
                                onValueChange = { baseCurrencySymbol = it }, 
                                label = { Text("Currency Symbol") }, 
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Right) }),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
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
                                    performSaveCompany()
                                }),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                            )
                        }
                    }
                }

                // Section 4: Security Control
                FormSectionCard(title = "Security Control") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SelectionRow("Tally Vault Password", tallyVaultEnabled) { tallyVaultEnabled = it }
                        SelectionRow("Control User Access", controlAccessEnabled) { controlAccessEnabled = it }
                    }
                }

                // Section 5: GST/HSN Statutory Defaults
                FormSectionCard(title = "GST/HSN Statutory Defaults") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SelectionRow("GST Applicability", gstApplicability) { gstApplicability = it }
                        if (gstApplicability == "Applicable") {
                            OutlinedTextField(
                                value = hsnNumber,
                                onValueChange = { hsnNumber = it },
                                label = { Text("HSN/SAC Number") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                            )
                            OutlinedTextField(
                                value = hsnDescription,
                                onValueChange = { hsnDescription = it },
                                label = { Text("HSN Description") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                            )
                            OutlinedTextField(
                                value = gstRate,
                                onValueChange = { gstRate = it },
                                label = { Text("Default GST Rate (%)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = WptColors.PrimaryAccent)
                            )
                            
                            Spacer(Modifier.height(4.dp))
                            Text("Type of Supply", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFF1D1B20))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = typeOfSupply == "Goods",
                                        onClick = { typeOfSupply = "Goods" },
                                        colors = RadioButtonDefaults.colors(selectedColor = WptColors.PrimaryAccent)
                                    )
                                    Text("Goods", style = MaterialTheme.typography.bodyMedium, color = WptColors.SecondaryText)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = typeOfSupply == "Services",
                                        onClick = { typeOfSupply = "Services" },
                                        colors = RadioButtonDefaults.colors(selectedColor = WptColors.PrimaryAccent)
                                    )
                                    Text("Services", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF49454F))
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = { performSaveCompany() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(bottom = 8.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WptColors.PrimaryAccent),
                    enabled = !isSaving
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = if (initialCompany != null) "Save Changes" else "Create Company",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
                
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}



@Composable
fun SelectionRow(label: String, selected: String, onSelect: (String) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1D1B20), fontWeight = FontWeight.SemiBold)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selected == "Yes", 
                    onClick = { onSelect("Yes") },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF7C4DFF))
                )
                Text("Yes", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF49454F))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selected == "No", 
                    onClick = { onSelect("No") },
                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF7C4DFF))
                )
                Text("No", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF49454F))
            }
        }
    }
}

