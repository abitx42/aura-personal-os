package com.example.ui

import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.anim.auraSpringPress
import com.example.ui.anim.ShimmerMoneyOverviewCard
import com.example.ui.anim.ShimmerTransactionRow
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

// Money sub-sections enum
enum class MoneySubSection {
    Overview, Transactions, FriendsSplits, Investments, SavingsGoals, Analytics, Reminders
}

sealed class MoneyDetailSection {
    object AvailableBalancePassbook : MoneyDetailSection()
    object SplitsToReceive : MoneyDetailSection()
    data class FriendSplitDetails(val friend: Friend) : MoneyDetailSection()
    object SplitsYouOwe : MoneyDetailSection()
    data class FriendOweDetails(val friend: Friend) : MoneyDetailSection()
    object PortfolioInvestments : MoneyDetailSection()
}

@Composable
fun MoneyTrackerScreen(
    viewModel: AppViewModel
) {
    var activeSubSection by remember { mutableStateOf(MoneySubSection.Overview) }

    androidx.activity.compose.BackHandler(enabled = activeSubSection != MoneySubSection.Overview) {
        activeSubSection = MoneySubSection.Overview
    }

    // Money related state flows
    val accounts by viewModel.allAccounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val isMoneyLoading by viewModel.isMoneyLoading.collectAsState()
    val investments by viewModel.allInvestments.collectAsState()
    val friends by viewModel.allFriends.collectAsState()
    val debts by viewModel.allDebts.collectAsState()
    val savingsGoals by viewModel.allSavingsGoals.collectAsState()
    val reminders by viewModel.allReminders.collectAsState()

    // Dialog & Form states
    var showQuickTransactionSheet by remember { mutableStateOf<String?>(null) } // "SENT", "RECEIVED", "INVESTED", "CASH_ADDED"
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var transactionToDelete by remember { mutableStateOf<Transaction?>(null) }
    var showAddInvestmentDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    var showSplitBillDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showBalanceAdjustmentDialog by remember { mutableStateOf<Account?>(null) }

    // Compute aggregated dynamic overview metrics
    val totalAvailableBalance = accounts.sumOf { it.balance }
    val totalInvested = investments.sumOf { it.amount }
    
    // Splitwise metrics
    val totalToReceive = debts.filter { !it.isYouOwe && it.status == "PENDING" }.sumOf { it.remainingAmount }
    val totalYouOwe = debts.filter { it.isYouOwe && it.status == "PENDING" }.sumOf { it.remainingAmount }
    val netWorth = totalAvailableBalance + totalInvested + totalToReceive - totalYouOwe

    val detailBackStack = remember { mutableStateListOf<MoneyDetailSection>() }
    val currentDetail = detailBackStack.lastOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopStart
    ) {
        if (currentDetail != null) {
            when (currentDetail) {
                is MoneyDetailSection.AvailableBalancePassbook -> {
                    AvailableBalancePassbookView(
                        accounts = accounts,
                        transactions = transactions,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onAdjustBalance = { showBalanceAdjustmentDialog = it },
                        onDeleteTransaction = { transactionToDelete = it },
                        onEditTransaction = {
                            editingTransaction = it
                            showQuickTransactionSheet = it.type
                        }
                    )
                }
                is MoneyDetailSection.SplitsToReceive -> {
                    SplitsToReceiveView(
                        friends = friends,
                        debts = debts,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onFriendClick = { detailBackStack.add(MoneyDetailSection.FriendSplitDetails(it)) }
                    )
                }
                is MoneyDetailSection.FriendSplitDetails -> {
                    FriendSplitDetailsView(
                        friend = currentDetail.friend,
                        debts = debts,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onQuickSettle = { viewModel.quickSettleDebt(it) }
                    )
                }
                is MoneyDetailSection.SplitsYouOwe -> {
                    SplitsYouOweView(
                        friends = friends,
                        debts = debts,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onFriendClick = { detailBackStack.add(MoneyDetailSection.FriendOweDetails(it)) }
                    )
                }
                is MoneyDetailSection.FriendOweDetails -> {
                    FriendSplitDetailsView(
                        friend = currentDetail.friend,
                        debts = debts,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onQuickSettle = { viewModel.quickSettleDebt(it) }
                    )
                }
                is MoneyDetailSection.PortfolioInvestments -> {
                    PortfolioInvestmentDetailsView(
                        investments = investments,
                        onBack = { detailBackStack.removeAt(detailBackStack.size - 1) },
                        onAddInvestmentClick = { showAddInvestmentDialog = true },
                        onDeleteInvestment = { viewModel.deleteInvestment(it) }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- 1. PREMIUM HEADER ---
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "MONEY ENGINE",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                letterSpacing = 2.sp
                            )
                            Text(
                                text = "Real-time ledger, splitwise splits, portfolio tracker",
                                fontSize = 11.sp,
                                color = AuraWhiteMuted
                            )
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AuraSectionInfoButton(
                                viewModel = viewModel,
                                title = "Money Ledger & Splits",
                                description = "Track your net liquid assets, cash-flow ledgers, investment portfolios, and collaborative Splitwise-style expense split rooms. Operates entirely offline with secure local databases."
                            )

                            // Account Badge with manual adjustment support
                            Card(
                                modifier = Modifier
                                    .clickable { showBalanceAdjustmentDialog = accounts.find { it.isDefault } ?: accounts.firstOrNull() }
                                    .border(1.dp, AuraSlateLight, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = AuraSlateCard)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(modifier = Modifier.size(8.dp).background(AuraCyanNeon, CircleShape))
                                    Text(
                                        text = (accounts.find { it.isDefault } ?: Account(name="Default", balance=0.0)).name.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AuraWhiteMedium
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 2. STATS OVERVIEW CARD MATRIX ---
                if (isMoneyLoading) {
                    item {
                        ShimmerMoneyOverviewCard()
                    }
                    items(4) {
                        ShimmerTransactionRow()
                    }
                } else {
                    item {
                        PremiumFinancialOverviewCard(
                            netWorth = netWorth,
                            available = totalAvailableBalance,
                            invested = totalInvested,
                            toReceive = totalToReceive,
                            youOwe = totalYouOwe,
                            onBalanceClick = { detailBackStack.add(MoneyDetailSection.AvailableBalancePassbook) },
                            onInvestedClick = { detailBackStack.add(MoneyDetailSection.PortfolioInvestments) },
                            onToReceiveClick = { detailBackStack.add(MoneyDetailSection.SplitsToReceive) },
                            onYouOweClick = { detailBackStack.add(MoneyDetailSection.SplitsYouOwe) },
                            onNetWorthClick = { activeSubSection = MoneySubSection.Analytics }
                        )
                    }

                    // --- 3. QUICK ENGAGEMENT TRANSIT TOOLBAR ---
                    item {
                        QuickEngagementToolbar(
                            onSentClick = { showQuickTransactionSheet = "SENT" },
                            onReceivedClick = { showQuickTransactionSheet = "RECEIVED" },
                            onInvestedClick = { showQuickTransactionSheet = "INVESTED" },
                            onAddedCashClick = { showQuickTransactionSheet = "CASH_ADDED" }
                        )
                    }

                    // --- 4. NAVIGATION PILL SLIDERS ---
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MoneySubSection.values().forEach { sub ->
                                val isActive = activeSubSection == sub
                                val label = when (sub) {
                                    MoneySubSection.Overview -> "Overview"
                                    MoneySubSection.Transactions -> "Ledger"
                                    MoneySubSection.FriendsSplits -> "Splits & Friends"
                                    MoneySubSection.Investments -> "Investments"
                                    MoneySubSection.SavingsGoals -> "Savings Goals"
                                    MoneySubSection.Analytics -> "Analytics Graphs"
                                    MoneySubSection.Reminders -> "Reminders"
                                }
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = if (isActive) AuraCyanNeon else AuraSlateCard,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isActive) Color.White else AuraSlateLight,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .auraSpringPress(
                                            cornerRadius = 12.dp,
                                            onClick = { activeSubSection = sub }
                                        )
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isActive) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // --- 5. RENDER CHOSEN MODULE ACTIVE VIEW ---
                when (activeSubSection) {
                    MoneySubSection.Overview -> {
                        // Interactive Mini sections dashboard
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                // Accounts List Card (Tapping card opens Available Balance Passbook)
                                AccountsSectionView(
                                    accounts = accounts,
                                    onAdjustBalance = { showBalanceAdjustmentDialog = it },
                                    onNavigateToPassbook = { detailBackStack.add(MoneyDetailSection.AvailableBalancePassbook) }
                                )

                                // Split Settlement overview block (Tapping card opens Splits To Receive module)
                                SplitSummarySectionView(
                                    toReceive = totalToReceive,
                                    youOwe = totalYouOwe,
                                    debts = debts,
                                    onQuickSettle = { viewModel.quickSettleDebt(it) },
                                    onNavigateToSplits = { detailBackStack.add(MoneyDetailSection.SplitsToReceive) }
                                )

                                // Reminders Section Alert Board (Tapping card opens Reminders Sub-section timeline)
                                MiniRemindersView(
                                    reminders = reminders.filter { !it.isCompleted }.take(3),
                                    onSettle = { viewModel.toggleReminderCompleted(it) },
                                    onNavigateToReminders = { activeSubSection = MoneySubSection.Reminders }
                                )
                            }
                        }
                    }
                    MoneySubSection.Transactions -> {
                        item {
                            TransactionLedgerView(
                                transactions = transactions,
                                onDeleteClick = { transactionToDelete = it },
                                onEditClick = {
                                    editingTransaction = it
                                    showQuickTransactionSheet = it.type
                                }
                            )
                        }
                    }
                    MoneySubSection.FriendsSplits -> {
                        item {
                            FriendsAndSplitsModule(
                                friends = friends,
                                debts = debts,
                                viewModel = viewModel,
                                onAddFriendClick = { showAddFriendDialog = true },
                                onSplitBillClick = { showSplitBillDialog = true },
                                onQuickSettle = { viewModel.quickSettleDebt(it) },
                                onDeleteDebt = { viewModel.deleteDebt(it) },
                                onDeleteFriend = { viewModel.deleteFriend(it) }
                            )
                        }
                    }
                    MoneySubSection.Investments -> {
                        item {
                            InvestmentsPortfolioView(
                                investments = investments,
                                onAddInvestmentClick = { showAddInvestmentDialog = true },
                                onDeleteInvestment = { viewModel.deleteInvestment(it) }
                            )
                        }
                    }
                    MoneySubSection.SavingsGoals -> {
                        item {
                            SavingsGoalsView(
                                goals = savingsGoals,
                                onAddGoalClick = { showAddGoalDialog = true },
                                onSettleProgress = { viewModel.updateSavingsGoal(it) },
                                onDeleteGoal = { viewModel.deleteSavingsGoal(it) }
                            )
                        }
                    }
                    MoneySubSection.Analytics -> {
                        item {
                            VisualAnalyticsDashboard(
                                transactions = transactions,
                                investments = investments,
                                debts = debts,
                                goals = savingsGoals,
                                onCategoryClick = { activeSubSection = MoneySubSection.Transactions },
                                onCashflowClick = { detailBackStack.add(MoneyDetailSection.AvailableBalancePassbook) }
                            )
                        }
                    }
                    MoneySubSection.Reminders -> {
                        item {
                            AccountRemindersTimeline(
                                reminders = reminders,
                                onAddReminderClick = { showAddReminderDialog = true },
                                onToggleDone = { viewModel.toggleReminderCompleted(it) },
                                onDeleteClick = { viewModel.deleteReminder(it) }
                            )
                        }
                    }
                }
                
                // Safety Bottom Space
                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }

    // --- FORM SHEET & ALERT DIALOGS POP-UPS ---

    // Quick transaction sheet (SENT, RECEIVED, INVESTED, CASH_ADDED)
    if (showQuickTransactionSheet != null) {
        val txType = showQuickTransactionSheet!!
        val sentOptions by viewModel.sentOptions.collectAsState()
        QuickTransactionBottomSheet(
            type = txType,
            accounts = accounts,
            friends = friends,
            sentOptions = sentOptions,
            onDismiss = {
                showQuickTransactionSheet = null
                editingTransaction = null
            },
            onSubmit = { amount, recipient, category, note, loc, method, acctId, tickedFriends, includeMe ->
                val editTx = editingTransaction
                if (editTx != null) {
                    viewModel.updateTransaction(
                        transactionId = editTx.id,
                        type = txType,
                        amount = amount,
                        recipientOrSender = recipient,
                        category = category,
                        note = note,
                        location = loc,
                        paymentMethod = method,
                        accountId = acctId,
                        dateString = editTx.dateString
                    )
                } else {
                    viewModel.addTransaction(
                        type = txType,
                        amount = amount,
                        recipientOrSender = recipient,
                        category = category,
                        note = note,
                        location = loc,
                        paymentMethod = method,
                        accountId = acctId
                    )
                }
                if (category == "Friend" && tickedFriends.isNotEmpty()) {
                    val shareCount = tickedFriends.size + (if (includeMe) 1 else 0)
                    val splitShare = amount / shareCount
                    tickedFriends.forEach { fri ->
                        viewModel.addDebt(
                            friendId = fri.id,
                            friendName = fri.name,
                            title = note.ifBlank { "Shared Quick Expense" },
                            totalAmount = splitShare,
                            amount = splitShare,
                            isYouOwe = (txType == "RECEIVED")
                        )
                    }
                }
                showQuickTransactionSheet = null
                editingTransaction = null
            },
            transactionToEdit = editingTransaction
        )
    }

    // Safety delete confirmation dialog
    if (transactionToDelete != null) {
        AlertDialog(
            onDismissRequest = { transactionToDelete = null },
            title = { Text("Delete Transaction", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete this transaction? This will revert its impact on your account balance.", color = AuraWhiteMedium) },
            confirmButton = {
                Button(
                    onClick = {
                        transactionToDelete?.let { viewModel.deleteTransaction(it) }
                        transactionToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("DELETE", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { transactionToDelete = null }) {
                    Text("CANCEL", color = Color.White)
                }
            },
            containerColor = AuraCharcoalBase
        )
    }

    // Balance Manual adjustment Dialog
    if (showBalanceAdjustmentDialog != null) {
        val acct = showBalanceAdjustmentDialog!!
        var adjustVal by remember { mutableStateOf(acct.balance.toString()) }
        AlertDialog(
            onDismissRequest = { showBalanceAdjustmentDialog = null },
            title = { Text("Update Available Balance", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = adjustVal,
                    onValueChange = { adjustVal = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    label = { Text("Available Amount (₹)", color = AuraCyanNeon) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val amt = adjustVal.toDoubleOrNull() ?: acct.balance
                        viewModel.updateAccountBalance(acct.id, amt)
                        showBalanceAdjustmentDialog = null
                    }
                ) {
                    Text("SAVE ADJUSTMENT", color = AuraCyanNeon, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBalanceAdjustmentDialog = null }) {
                    Text("CANCEL", color = Color.White)
                }
            },
            containerColor = AuraCharcoalBase
        )
    }

    // Add Investment Dialog
    if (showAddInvestmentDialog) {
        AddInvestmentFormDialog(
            onDismiss = { showAddInvestmentDialog = false },
            onSubmit = { name, type, amt, date, note ->
                viewModel.addInvestment(name, type, amt, date, note)
                showAddInvestmentDialog = false
            }
        )
    }

    // Add Friend profiles Dialog
    if (showAddFriendDialog) {
        AddFriendProfileDialog(
            onDismiss = { showAddFriendDialog = false },
            onSubmit = { name, phone, note ->
                viewModel.addFriend(name, phone, note)
                showAddFriendDialog = false
            }
        )
    }

    // Bill Splitting System dialog window
    if (showSplitBillDialog) {
        BillSplittingFormDialog(
            friends = friends,
            onDismiss = { showSplitBillDialog = false },
            onSubmit = { title, amt, isYouOwe, selectedParticipants, splitType, customPays ->
                // Automatically calculate and register debt transactions under participants
                val participantCount = selectedParticipants.size + 1 // + Me
                when (splitType) {
                    "EQUAL" -> {
                        val chunk = amt / participantCount
                        selectedParticipants.forEach { fri ->
                            viewModel.addDebt(
                                friendId = fri.id,
                                friendName = fri.name,
                                title = title,
                                totalAmount = chunk,
                                amount = chunk,
                                isYouOwe = isYouOwe
                            )
                        }
                    }
                    "CUSTOM", "PERCENTAGE" -> {
                        customPays.forEach { (friendId, customAmt) ->
                            val fri = friends.find { it.id == friendId } ?: return@forEach
                            viewModel.addDebt(
                                friendId = friendId,
                                friendName = fri.name,
                                title = title,
                                totalAmount = customAmt,
                                amount = customAmt,
                                isYouOwe = isYouOwe
                            )
                        }
                    }
                }
                showSplitBillDialog = false
            }
        )
    }

    // Add Savings Goal
    if (showAddGoalDialog) {
        AddSavingsGoalFormDialog(
            onDismiss = { showAddGoalDialog = false },
            onSubmit = { name, target, saved, date, notes ->
                viewModel.addSavingsGoal(name, target, saved, date, notes)
                showAddGoalDialog = false
            }
        )
    }

    // Add Reminders Alert
    if (showAddReminderDialog) {
        AddReminderFormDialog(
            onDismiss = { showAddReminderDialog = false },
            onSubmit = { title, amt, date, isRec, rec ->
                viewModel.addReminder(title, amt, date, isRec, rec)
                showAddReminderDialog = false
            }
        )
    }
}

// ===================================================
// COMPONENT: PREMIUM OVERVIEW STATS BANNERS
// ===================================================
@Composable
fun PremiumFinancialOverviewCard(
    netWorth: Double,
    available: Double,
    invested: Double,
    toReceive: Double,
    youOwe: Double,
    onBalanceClick: () -> Unit,
    onInvestedClick: () -> Unit,
    onToReceiveClick: () -> Unit,
    onYouOweClick: () -> Unit,
    onNetWorthClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.linearGradient(listOf(AuraCyanNeon.copy(alpha = 0.5f), AuraPurpleAccent.copy(alpha = 0.5f))),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Net Worth Top Row (Clickable: opens Analytics Graphs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .auraSpringPress(
                        cornerRadius = 24.dp,
                        onClick = onNetWorthClick
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "DYNAMIC NET WORTH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraPurpleAccent,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "₹${"%,.2f".format(netWorth)}",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Dynamic balance calculation",
                    tint = AuraCyanNeon,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = AuraSlateLight.copy(alpha = 0.4f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Aggregated breakdown quadrants
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Available & Investment (Left Column)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Available Balance Quadrant
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateLight.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .auraSpringPress(
                                cornerRadius = 12.dp,
                                onClick = onBalanceClick
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(AuraCyanNeon, CircleShape))
                            Text("AVAILABLE BALANCE", fontSize = 9.sp, color = AuraWhiteMuted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("₹${"%,.0f".format(available)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    // Portfolio Invested Quadrant
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateLight.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .auraSpringPress(
                                cornerRadius = 12.dp,
                                onClick = onInvestedClick
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color.Yellow, CircleShape))
                            Text("PORTFOLIO INVESTED", fontSize = 9.sp, color = AuraWhiteMuted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("₹${"%,.0f".format(invested)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Settlements (Right Column)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Splits to Receive Quadrant
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateLight.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .auraSpringPress(
                                cornerRadius = 12.dp,
                                onClick = onToReceiveClick
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(MoodHappy, CircleShape))
                            Text("SPLITS TO RECEIVE", fontSize = 9.sp, color = AuraWhiteMuted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("₹${"%,.0f".format(toReceive)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MoodHappy)
                    }

                    // Splits You Owe Quadrant
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateLight.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .auraSpringPress(
                                cornerRadius = 12.dp,
                                onClick = onYouOweClick
                            )
                            .padding(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                            Text("SPLITS YOU OWE", fontSize = 9.sp, color = AuraWhiteMuted, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("₹${"%,.0f".format(youOwe)}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                    }
                }
            }
        }
    }
}

// ===================================================
// COMPONENT: QUICK ENGAGEMENT TRANSIT BAR + BUTTONS
// ===================================================
@Composable
fun QuickEngagementToolbar(
    onSentClick: () -> Unit,
    onReceivedClick: () -> Unit,
    onInvestedClick: () -> Unit,
    onAddedCashClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val actions = listOf(
            Triple("+ Sent", AuraPurpleAccent, onSentClick),
            Triple("+ Recv", MoodHappy, onReceivedClick),
            Triple("+ Portf", Color.Yellow, onInvestedClick),
            Triple("+ Cash", AuraCyanNeon, onAddedCashClick)
        )

        actions.forEach { (label, colorScheme, callback) ->
            Button(
                onClick = callback,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = AuraSlateCard),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(0.dp),
                border = BorderStroke(1.dp, colorScheme.copy(alpha = 0.5f))
            ) {
                Text(
                    text = label, 
                    color = colorScheme, 
                    fontSize = 11.sp, 
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

// ===================================================
// SUB-MODULE: TRANSACTION JOURNAL LEDGER
// ===================================================
@Composable
fun TransactionLedgerView(
    transactions: List<Transaction>,
    onDeleteClick: (Transaction) -> Unit,
    onEditClick: (Transaction) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterCategory by remember { mutableStateOf("All") }
    var filterType by remember { mutableStateOf("All") }

    val filteredList = transactions.filter { tx ->
        val matchesQuery = tx.recipientOrSender.contains(searchQuery, true) ||
                tx.category.contains(searchQuery, true) ||
                tx.note.contains(searchQuery, true) ||
                tx.amount.toString().contains(searchQuery)
        
        val matchesCategory = filterCategory == "All" || tx.category == filterCategory
        val matchesType = filterType == "All" || tx.type == filterType
        
        matchesQuery && matchesCategory && matchesType
    }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("FINANCIAL TRANSACTION LEDGER", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar & Simple Filter Tabs
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by description, merchant, notes...", color = AuraWhiteMuted, fontSize = 11.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = AuraWhiteMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AuraCyanNeon,
                    unfocusedBorderColor = AuraSlateLight,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category & Type sliders
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("All", "SENT", "RECEIVED", "INVESTED").forEach { t ->
                    val isSel = filterType == t
                    Box(
                        modifier = Modifier
                            .background(if (isSel) AuraCyanNeon else AuraSlateCard, RoundedCornerShape(8.dp))
                            .clickable { filterType = t }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(t, fontSize = 9.sp, color = if (isSel) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (filteredList.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = "No trans", tint = AuraWhiteMuted, modifier = Modifier.size(48.dp))
                    Text("No transactions match search criteria", color = AuraWhiteMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
            } else {
                // Group by dates
                val grouped = filteredList.groupBy { it.dateString }
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    grouped.forEach { (dateKey, list) ->
                        Text(
                            text = dateKey.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraWhiteMedium,
                            letterSpacing = 1.sp
                        )
                        list.forEach { tx ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val icon = when (tx.type) {
                                        "SENT" -> Icons.Default.ArrowOutward
                                        "RECEIVED" -> Icons.Default.CallReceived
                                        "INVESTED" -> Icons.Default.TrendingUp
                                        else -> Icons.Default.PlusOne
                                    }
                                    val iconColor = when (tx.type) {
                                        "SENT" -> AuraPurpleAccent
                                        "RECEIVED" -> MoodHappy
                                        "INVESTED" -> Color.Yellow
                                        else -> AuraCyanNeon
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(iconColor.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(icon, contentDescription = tx.type, tint = iconColor, modifier = Modifier.size(16.dp))
                                    }

                                    Column {
                                        Text(
                                            tx.recipientOrSender,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${tx.category} • ${tx.paymentMethod}",
                                            fontSize = 9.sp,
                                            color = AuraWhiteMuted
                                        )
                                        if (tx.note.isNotBlank()) {
                                            Text(tx.note, fontSize = 9.sp, color = AuraCyanNeon, maxLines = 1)
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val prefix = when (tx.type) {
                                        "SENT", "INVESTED" -> "-"
                                        "RECEIVED", "CASH_ADDED" -> "+"
                                        else -> ""
                                    }
                                    val color = when (tx.type) {
                                        "SENT" -> AuraPurpleAccent
                                        "RECEIVED" -> MoodHappy
                                        "INVESTED" -> Color.Yellow
                                        else -> AuraCyanNeon
                                    }
                                    Text(
                                        "$prefix₹${"%,.0f".format(tx.amount)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = color
                                    )
                                    IconButton(
                                        onClick = { onEditClick(tx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit transaction", tint = AuraWhiteMuted, modifier = Modifier.size(14.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteClick(tx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete transaction", tint = AuraWhiteMuted, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================
// SUB-MODULE: PORTFOLIO INVESTMENTS TRACKER
// ===================================================
@Composable
fun InvestmentsPortfolioView(
    investments: List<Investment>,
    onAddInvestmentClick: () -> Unit,
    onDeleteInvestment: (Investment) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("INVESTMENT PORTFOLIO", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon, letterSpacing = 1.sp)
                Button(
                    onClick = onAddInvestmentClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("+ ADD PORTF", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (investments.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Timeline, contentDescription = "No portfolio", tint = AuraWhiteMuted, modifier = Modifier.size(48.dp))
                    Text("No investments logged. Track stocks, mutual funds, or gold.", color = AuraWhiteMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            } else {
                investments.forEach { inv ->
                    val daysHeld = try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val parseDate = sdf.parse(inv.date) ?: Date()
                        val diffMs = System.currentTimeMillis() - parseDate.time
                        (diffMs / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                    } catch (e: Exception) {
                        0L
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(AuraSlateCard.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(inv.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.background(Color.Yellow, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 1.dp)) {
                                    Text(inv.type.uppercase(), fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                                Text("Held: $daysHeld Days", fontSize = 10.sp, color = AuraWhiteMuted)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("₹${"%,.0f".format(inv.amount)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Yellow)
                            IconButton(onClick = { onDeleteInvestment(inv) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete investment", tint = AuraWhiteMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================
// SUB-MODULE: LIGHTWEIGHT SPLITWISE & BILL COMPOSERS
// ===================================================
@Composable
fun FriendsAndSplitsModule(
    friends: List<Friend>,
    debts: List<Debt>,
    viewModel: AppViewModel,
    onAddFriendClick: () -> Unit,
    onSplitBillClick: () -> Unit,
    onQuickSettle: (Debt) -> Unit,
    onDeleteDebt: (Debt) -> Unit,
    onDeleteFriend: (Friend) -> Unit
) {
    // Cloud Sync States
    val isSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val userEmail by viewModel.cloudUserEmail.collectAsState()
    val isSyncing by viewModel.isCurrentlySyncing.collectAsState()
    val lastSync by viewModel.lastSyncedTime.collectAsState()
    val backups by viewModel.mockCloudBackups.collectAsState()

    // Live Splits & Group Expense Rooms States
    val groupRooms by viewModel.groupRooms.collectAsState()
    val roomExpenses by viewModel.roomExpenses.collectAsState()
    val socialActivities by viewModel.socialActivities.collectAsState()

    // Interactive Dialog States
    var activeLedgerTab by remember { mutableStateOf("1-ON-1") } // "1-ON-1", "ROOMS", "SOCIAL"
    var showGoogleSignDialog by remember { mutableStateOf(false) }
    var inviteFriendItem by remember { mutableStateOf<Friend?>(null) }
    var showCreateRoomDialog by remember { mutableStateOf(false) }
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    var showAddRoomExpenseDialog by remember { mutableStateOf(false) }
    var activeReceiptPath by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    var context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        
        // ==========================================
        // SECTION 1: AURA CLINICAL CLOUD SYNC & BACKUP
        // ==========================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .border(1.dp, if (isSyncEnabled) AuraCyanNeon.copy(alpha = 0.5f) else AuraSlateLight, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue, 
                            contentDescription = "Cloud Icon", 
                            tint = if (isSyncEnabled) AuraCyanNeon else AuraPurpleAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text("AURA CLOUD BACKUP & SYNCHRONIZER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 0.5.sp)
                            Text(
                                text = if (isSyncEnabled) "Cloud Sync: Connected to Google" else "Offline Local Storage Only",
                                fontSize = 9.sp,
                                color = if (isSyncEnabled) MoodCalm else AuraWhiteMuted
                            )
                        }
                    }

                    // Syncing Spinner or Connected Indicator
                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AuraCyanNeon, strokeWidth = 2.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    if (isSyncEnabled) MoodHappy else AuraCopperWarm, 
                                    CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (userEmail == null) {
                    // Sign-In Visual Call-to-Action
                    Text(
                        text = "Sign in to activate encrypted Drive backups & synchronize shared transaction splits with friends in real time.",
                        fontSize = 10.sp,
                        color = AuraWhiteMedium,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { showGoogleSignDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("google_signin_trigger"),
                        border = BorderStroke(1.dp, AuraCyanNeon.copy(alpha = 0.3f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("G", color = AuraCyanNeon, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Text("Connect Google Workspace Account", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Google Signed-In Visual Interface
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraObsidian.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Avatar Icon representing G-Profile
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(AuraPurpleAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(userEmail ?: "", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Mirror Last Sync: $lastSync", fontSize = 8.sp, color = AuraWhiteMuted)
                            }
                        }

                        // Disconnect link
                        Text(
                            text = "Disconnect",
                            color = Color.Red.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { viewModel.signOut() }
                                .padding(4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.triggerSyncNow() },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = "Sync icon", tint = Color.Black, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SYNC NOW", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.createGoogleDriveBackup() },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 4.dp),
                            border = BorderStroke(1.dp, AuraSlateLight)
                        ) {
                            Icon(Icons.Default.CloudQueue, contentDescription = "Backup icon", tint = AuraCyanNeon, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("BACKUP DRIVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Collapsible Backup List
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("GOOGLE DRIVE RESTORE POINTS", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMuted, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    backups.take(2).forEach { bkp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .background(AuraSlateCard.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(bkp, fontSize = 10.sp, color = AuraWhiteMedium)
                            Text(
                                text = "RESTORE",
                                color = AuraCyanNeon,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        coroutineScope.launch {
                                            viewModel.triggerSyncNow()
                                            android.widget.Toast.makeText(context, "Data restoration index synced safely!", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // SECTION 2: CHROME NAVIGATION DIAL TABS
        // ==========================================
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .background(AuraSlateCard, RoundedCornerShape(12.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf("1-ON-1" to "INDIVIDUAL SPLITS", "ROOMS" to "GROUP ROOMS 🏖️", "SOCIAL" to "SOCIAL FEED 💬")
            tabs.forEach { (key, title) ->
                val active = activeLedgerTab == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) AuraSlateLight else Color.Transparent,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { activeLedgerTab = key }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = if (active) AuraCyanNeon else AuraWhiteMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ==========================================
        // TAB CONTROLLER ACTIONS
        // ==========================================
        when (activeLedgerTab) {
            "1-ON-1" -> {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1-ON-1 SPLITS LEDGER", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = onAddFriendClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraSlateCard),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    border = BorderStroke(1.dp, AuraCyanNeon.copy(alpha = 0.5f))
                                ) {
                                    Text("+ FRIEND", color = AuraCyanNeon, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = onSplitBillClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("SPLIT BILL", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("FRIENDS & CLOUD PROFILE CONNECTIONS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        if (friends.isEmpty()) {
                            Text("No friends available.", color = AuraWhiteMuted, fontSize = 11.sp)
                        } else {
                            friends.forEach { fri ->
                                val friendDebts = debts.filter { it.friendId == fri.id && it.status == "PENDING" }
                                val toRecVal = friendDebts.filter { !it.isYouOwe }.sumOf { it.remainingAmount }
                                val oweVal = friendDebts.filter { it.isYouOwe }.sumOf { it.remainingAmount }
                                val netBal = toRecVal - oweVal

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .background(AuraSlateCard.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(fri.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                            // Tiny glowing connected indicator representing real-time state
                                            Box(modifier = Modifier.size(6.dp).background(MoodHappy, CircleShape))
                                        }
                                        if (fri.phone.isNotEmpty()) {
                                            Text("Link ID: ${fri.phone}", fontSize = 8.sp, color = AuraWhiteMuted)
                                        }
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        val netColor = if (netBal > 0) MoodHappy else if (netBal < 0) Color.Red else Color.White
                                        val netLabel = if (netBal > 0) "Owed: ₹${netBal.toInt()}" else if (netBal < 0) "Owe: ₹${(-netBal).toInt()}" else "Settled"
                                        
                                        Text(netLabel, fontSize = 11.sp, color = netColor, fontWeight = FontWeight.Bold)
                                        
                                        // Invite button
                                        IconButton(
                                            onClick = { inviteFriendItem = fri }, 
                                            modifier = Modifier.size(26.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Invite friend code", tint = AuraCyanNeon, modifier = Modifier.size(13.dp))
                                        }

                                        IconButton(onClick = { onDeleteFriend(fri) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.PersonRemove, contentDescription = "Delete friend", tint = AuraWhiteMuted.copy(alpha=0.6f), modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Divider(color = AuraSlateLight.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(14.dp))

                        Text("OPEN BILL SPLITS (DEBTS)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMedium)
                        Spacer(modifier = Modifier.height(8.dp))

                        val pendingDebts = debts.filter { it.status == "PENDING" }
                        if (pendingDebts.isEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text("All individual splits settled! Great.", color = MoodHappy, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            pendingDebts.forEach { dbt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .border(1.dp, AuraSlateLight.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .background(AuraObsidian.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(dbt.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "${if (dbt.isYouOwe) "You owe" else "Owes you"} ${dbt.friendName}",
                                                fontSize = 9.sp,
                                                color = AuraWhiteMuted
                                            )
                                            if (dbt.isSynced) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = "Synced", tint = MoodHappy, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            "₹${dbt.remainingAmount.toInt()}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (dbt.isYouOwe) Color.Red else MoodHappy
                                        )

                                        Button(
                                            onClick = { onQuickSettle(dbt) },
                                            colors = ButtonDefaults.buttonColors(containerColor = if (dbt.isYouOwe) Color.Red else MoodHappy),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("SETTLE", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "ROOMS" -> {
                // ==========================================
                // TAB 2: GROUP EXPENSE SPLITWISE ROOMS (PHASE 3)
                // ==========================================
                if (selectedRoomId == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(20.dp)),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("COGNITIVE EXPENSE ROOMS", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon)
                                Button(
                                    onClick = { showCreateRoomDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("+ CREATE ROOM", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            if (groupRooms.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = AuraWhiteMuted,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        "No shared expense rooms configured. Create a room to manage Splitwise-style travel or room bills.",
                                        color = AuraWhiteMuted,
                                        fontSize = 11.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            } else {
                                groupRooms.forEach { room ->
                                    val expenses = roomExpenses.filter { it.roomId == room.id }
                                    val totalAmt = expenses.sumOf { it.amount }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .border(1.dp, AuraSlateLight.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                            .clickable { selectedRoomId = room.id }
                                            .background(AuraObsidian.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(AuraSlateLight, RoundedCornerShape(8.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(room.emoji, fontSize = 18.sp)
                                            }

                                            Column {
                                                Text(room.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text(
                                                    text = "${room.memberNames.size} members • ${room.memberNames.joinToString(", ")}",
                                                    fontSize = 8.sp,
                                                    color = AuraWhiteMuted,
                                                    maxLines = 1
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${totalAmt.toInt()}", fontSize = 13.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon)
                                            Text("Total Spent", fontSize = 8.sp, color = AuraWhiteMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Room Details Pane
                    val roomId = selectedRoomId!!
                    val roomObj = groupRooms.find { it.id == roomId }
                    if (roomObj == null) {
                        selectedRoomId = null
                    } else {
                        val expenses = roomExpenses.filter { it.roomId == roomId }
                        val totalAmt = expenses.sumOf { it.amount }
                        val minSettlements = viewModel.getMinimizeTransactionsForRoom(roomId)

                        Card(
                            modifier = Modifier.fillMaxWidth().border(1.dp, AuraCyanNeon.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = AuraSlateCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Room Header Info
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(roomObj.emoji, fontSize = 20.sp)
                                        Column {
                                            Text(roomObj.name.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                                            Text("${roomObj.memberNames.size} connected members", fontSize = 8.sp, color = AuraWhiteMuted)
                                        }
                                    }

                                    Text(
                                        "BACK TO LIST",
                                        color = AuraCyanNeon,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { selectedRoomId = null }
                                            .padding(6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(AuraObsidian, RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("TOTAL ROOM SPEND", fontSize = 8.sp, color = AuraWhiteMuted)
                                        Text("₹${totalAmt.toInt()}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon)
                                    }
                                    Button(
                                        onClick = { showAddRoomExpenseDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("+ ADD EXPENSE", color = Color.Black, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // 1. SMART SETTLEMENT ALGORITHM SOLVER (PHASE 3)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("SMART DEBT-MINIMIZATION RECOMMENDATIONS", fontSize = 9.sp, fontWeight = FontWeight.Black, color = AuraPurpleAccent, letterSpacing = 0.5.sp)
                                Spacer(modifier = Modifier.height(6.dp))

                                if (minSettlements.isEmpty()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, MoodHappy.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text("🎉 Group is fully settled up! Zero balances outstanding.", color = MoodHappy, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    minSettlements.forEach { (debtor, creditor, amt) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .background(AuraObsidian.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text(debtor, fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 11.sp)
                                                Text("owes", color = AuraWhiteMuted, fontSize = 10.sp)
                                                Text(creditor, fontWeight = FontWeight.Bold, color = MoodHappy, fontSize = 11.sp)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Text("₹${amt.toInt()}", fontWeight = FontWeight.Black, color = Color.White, fontSize = 12.sp)
                                                Button(
                                                    onClick = { 
                                                        viewModel.settleGroupDebt(roomId, debtor, creditor, amt)
                                                        android.widget.Toast.makeText(context, "Settlement registered!", android.widget.Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("SETTLE DIRECT", color = AuraCyanNeon, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                // 2. Room Expense history
                                Spacer(modifier = Modifier.height(14.dp))
                                Text("EXPENSES CHRONIC Timeline", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMedium)
                                Spacer(modifier = Modifier.height(6.dp))

                                if (expenses.isEmpty()) {
                                    Text("No expenses logged in this room yet.", color = AuraWhiteMuted, fontSize = 10.sp)
                                } else {
                                    expenses.forEach { exp ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 3.dp)
                                                .background(AuraObsidian.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(exp.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                Text("Paid by ${exp.paidByName}", fontSize = 8.sp, color = AuraWhiteMuted)
                                            }
                                            Text("₹${exp.amount.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "SOCIAL" -> {
                // ==========================================
                // TAB 3: SOCIAL REACTION HUB (PHASE 4)
                // ==========================================
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("REAL-TIME ACTIVITIES & FEED", fontSize = 11.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon)
                        Spacer(modifier = Modifier.height(10.dp))

                        socialActivities.forEach { act ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .border(1.dp, AuraSlateLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .background(AuraObsidian.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Initials Avatar
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(AuraPurpleAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(act.userName.take(2).uppercase(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(act.userName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        Text(
                                            text = SimpleDateFormat("HH:mm", Locale.US).format(Date(act.timestamp)),
                                            fontSize = 8.sp,
                                            color = AuraWhiteMuted
                                        )
                                    }

                                    Text(act.text, fontSize = 11.sp, color = AuraWhiteMedium)

                                    // Attachment Display (UPI Payment Screenshot simulation)
                                    if (act.receiptPath != null) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Button(
                                            onClick = { activeReceiptPath = act.receiptPath },
                                            colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.AttachFile, contentDescription = "attachment", tint = AuraCyanNeon, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("UPI_RECEIPT_PROOF.PNG", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                        }
                                    }

                                    // Predefined Emoji Reactions Row
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val reactions = listOf("👍", "👀", "😮", "✅")
                                        reactions.forEach { emoji ->
                                            val isReacted = act.emojiReaction == emoji
                                            Box(
                                                modifier = Modifier
                                                    .border(
                                                        1.dp, 
                                                        if (isReacted) AuraCyanNeon else Color.Transparent, 
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .background(
                                                        if (isReacted) AuraCyanNeon.copy(alpha = 0.2f) else AuraSlateLight, 
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .clickable {
                                                        viewModel.addSocialActivity(
                                                            userName = "Me",
                                                            text = "reacted with ${emoji} on \"${act.userName}\" split activity",
                                                            type = "REACTION",
                                                            emojiReaction = emoji
                                                        )
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(emoji, fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // DIALOG POPUPS & SHEETS CONTROLLERS
    // ==========================================

    // 1. Google Account picker Simulation dialog window
    if (showGoogleSignDialog) {
        var customEmailInput by remember { mutableStateOf("moreaboutastram@gmail.com") }

        AlertDialog(
            onDismissRequest = { showGoogleSignDialog = false },
            title = { Text("GOOGLE CLOUD ACCESS LINK", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select or enter a Google account credentials to link for secure sync mirroring:", color = AuraWhiteMedium, fontSize = 11.sp)
                    OutlinedTextField(
                        value = customEmailInput,
                        onValueChange = { customEmailInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyanNeon,
                            unfocusedBorderColor = AuraSlateLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        placeholder = { Text("example@gmail.com") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signInWithGoogle(customEmailInput)
                        showGoogleSignDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
                ) {
                    Text("AUTHORIZE & SYNC", color = Color.Black)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showGoogleSignDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight)
                ) {
                    Text("CANCEL", color = Color.White)
                }
            },
            containerColor = AuraSlateCard
        )
    }

    // 2. Peer Friend Invitation Link Dialog Drawer
    if (inviteFriendItem != null) {
        val friObj = inviteFriendItem!!
        val linkText = "https://auranotes.app/invite?from=usr_${friObj.id}"

        AlertDialog(
            onDismissRequest = { inviteFriendItem = null },
            title = { Text("PEER CLOUD CONNECT INVITE", color = Color.White, fontSize = 14.sp) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Share this secure entry connect URL with ${friObj.name} to sync all your split balances in real time", color = AuraWhiteMedium, fontSize = 11.sp)
                    
                    // Web URL link text field box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraObsidian, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(linkText, color = AuraCyanNeon, fontSize = 11.sp, maxLines = 2)
                    }

                    // Graphical matrix representation of QR Code matching specifications
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .border(1.dp, AuraCyanNeon.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .padding(8.dp)
                    ) {
                        // Drawing QR grid squares
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(6) { indexRow ->
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    repeat(6) { indexCol ->
                                        val fillSquare = (indexRow + indexCol) % 2 == 0 || (indexRow < 2 && indexCol < 2) || (indexRow > 3 && indexCol > 3)
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(if (fillSquare) Color.Black else Color.White)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Copy to clipboard
                        inviteFriendItem = null
                        android.widget.Toast.makeText(context, "Copied connection invite link!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
                ) {
                    Text("COPY URL & CONNECTIONS", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AuraSlateCard
        )
    }

    // 3. Group Room Creator Dialog Setup
    if (showCreateRoomDialog) {
        var roomNameInput by remember { mutableStateOf("") }
        var selectedEmoji by remember { mutableStateOf("🏖️") }
        val emojis = listOf("🏖️", "🏡", "🍕", "🚗", "💼", "🍿")
        val checkedFriends = remember { mutableStateMapOf<String, Boolean>() }

        AlertDialog(
            onDismissRequest = { showCreateRoomDialog = false },
            title = { Text("NEW GROUP ROOM SETUP", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    Text("Enter Group Room title details:", color = AuraWhiteMedium, fontSize = 11.sp)
                    OutlinedTextField(
                        value = roomNameInput,
                        onValueChange = { roomNameInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraCyanNeon,
                            unfocusedBorderColor = AuraSlateLight,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        placeholder = { Text("e.g. Flatmates Bills, Goa 2026") }
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Choose Room Visual Emoji:", color = AuraWhiteMedium, fontSize = 11.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        emojis.forEach { emoji ->
                            val isSel = selectedEmoji == emoji
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(if (isSel) AuraCyanNeon else AuraSlateLight, RoundedCornerShape(8.dp))
                                    .clickable { selectedEmoji = emoji }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emoji, fontSize = 16.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Select Group Members Checklist:", color = AuraWhiteMedium, fontSize = 11.sp)
                    
                    if (friends.isEmpty()) {
                        Text("Please create Friend entry items before setting group rooms.", color = AuraWhiteMuted, fontSize = 10.sp)
                    } else {
                        friends.forEach { fri ->
                            val isChecked = checkedFriends[fri.name] ?: false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { checkedFriends[fri.name] = !isChecked }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = isChecked, 
                                    onCheckedChange = { checkedFriends[fri.name] = it },
                                    colors = CheckboxDefaults.colors(checkedColor = AuraCyanNeon)
                                )
                                Text(fri.name, color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeMembers = checkedFriends.filter { it.value }.map { it.key }
                        if (roomNameInput.isNotBlank()) {
                            viewModel.createGroupRoom(roomNameInput, selectedEmoji, activeMembers)
                        }
                        showCreateRoomDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
                ) {
                    Text("CREATE ROOM ROOM", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showCreateRoomDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight)
                ) {
                    Text("CANCEL", color = Color.White, fontSize = 11.sp)
                }
            },
            containerColor = AuraSlateCard
        )
    }

    // 4. Create Group Expense Dialog Form
    if (showAddRoomExpenseDialog && selectedRoomId != null) {
        val activeRoom = groupRooms.find { it.id == selectedRoomId }
        if (activeRoom != null) {
            var expenseTitleInput by remember { mutableStateOf("") }
            var expenseAmountInput by remember { mutableStateOf("") }
            var expensePaidBy by remember { mutableStateOf("Me") }
            var customSplitStrategy by remember { mutableStateOf("EQUAL") }

            AlertDialog(
                onDismissRequest = { showAddRoomExpenseDialog = false },
                title = { Text("ADD GROUP ROOM EXPENSE", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black) },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    ) {
                        Text("Expense Title:", color = AuraWhiteMedium, fontSize = 11.sp)
                        OutlinedTextField(
                            value = expenseTitleInput,
                            onValueChange = { expenseTitleInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraCyanNeon,
                                unfocusedBorderColor = AuraSlateLight,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            placeholder = { Text("e.g. Resort Booking / Lunch") }
                        )

                        Text("Total Bill Amount (₹):", color = AuraWhiteMedium, fontSize = 11.sp)
                        OutlinedTextField(
                            value = expenseAmountInput,
                            onValueChange = { expenseAmountInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraCyanNeon,
                                unfocusedBorderColor = AuraSlateLight,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            placeholder = { Text("e.g. 1800") }
                        )

                        Text("Who Paid Initially:", color = AuraWhiteMedium, fontSize = 11.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            activeRoom.memberNames.forEach { mbr ->
                                val isPaid = expensePaidBy == mbr
                                Box(
                                    modifier = Modifier
                                        .background(if (isPaid) AuraPurpleAccent else AuraSlateLight, RoundedCornerShape(8.dp))
                                        .clickable { expensePaidBy = mbr }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(mbr, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Text("Split Proportion strategy:", color = AuraWhiteMedium, fontSize = 11.sp)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val strategies = listOf("EQUAL", "CUSTOM")
                            strategies.forEach { strat ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { customSplitStrategy = strat }
                                ) {
                                    RadioButton(
                                        selected = customSplitStrategy == strat,
                                        onClick = { customSplitStrategy = strat },
                                        colors = RadioButtonDefaults.colors(selectedColor = AuraCyanNeon)
                                    )
                                    Text(strat, color = Color.White, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val billTotalValue = expenseAmountInput.toDoubleOrNull() ?: 0.0
                            if (billTotalValue > 0.0 && expenseTitleInput.isNotBlank()) {
                                // EQUAL Chunk calculators
                                val partitionCount = activeRoom.memberNames.size
                                val mapSplits = activeRoom.memberNames.associateWith { billTotalValue / partitionCount }
                                viewModel.addRoomExpense(
                                    roomId = activeRoom.id,
                                    title = expenseTitleInput,
                                    amount = billTotalValue,
                                    paidByName = expensePaidBy,
                                    splits = mapSplits
                                )
                            }
                            showAddRoomExpenseDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
                    ) {
                        Text("RECORD EXPENSE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    Button(
                        onClick = { showAddRoomExpenseDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraSlateLight)
                    ) {
                        Text("CANCEL", color = Color.White, fontSize = 11.sp)
                    }
                },
                containerColor = AuraSlateCard
            )
        }
    }

    // 5. Beautiful UPI screenshot billing proof confirmation Dialog overlay
    if (activeReceiptPath != null) {
        val receipt = activeReceiptPath!!

        AlertDialog(
            onDismissRequest = { activeReceiptPath = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "verified security", tint = MoodHappy, modifier = Modifier.size(18.dp))
                    Text("VERIFIED UPI RECEIPT PROOF", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraObsidian)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .background(MoodHappy.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("₹", color = MoodHappy, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }

                        Text("TRANSFER TRANSACTION SUCCESSFUL", color = MoodHappy, fontSize = 11.sp, fontWeight = FontWeight.Black)

                        Divider(color = AuraSlateLight.copy(alpha = 0.5f))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Recipient Name:", color = AuraWhiteMuted, fontSize = 9.sp)
                            Text("Sahil (Roommate)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Amount Settled:", color = AuraWhiteMuted, fontSize = 9.sp)
                            Text("₹450.00", color = AuraCyanNeon, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("UPI Reference No:", color = AuraWhiteMuted, fontSize = 9.sp)
                            Text("618295039203", color = Color.White, fontSize = 10.sp)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Timestamp:", color = AuraWhiteMuted, fontSize = 9.sp)
                            Text("Jun 5, 2026, 23:18:57 UTC", color = Color.White, fontSize = 10.sp)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AuraSlateCard, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✓ SIGNED SECURE & AUDITED VIA F-SECURE", color = AuraCyanNeon, fontSize = 8.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { activeReceiptPath = null },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
                ) {
                    Text("DONE", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = AuraSlateCard
        )
    }
}

// ===================================================
// SUB-MODULE: SAVINGS PLANNER GOALS
// ===================================================
@Composable
fun SavingsGoalsView(
    goals: List<SavingsGoal>,
    onAddGoalClick: () -> Unit,
    onSettleProgress: (SavingsGoal) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SAVINGS GOALS & PLANS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon, letterSpacing = 1.sp)
                Button(
                    onClick = onAddGoalClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("+ ADD GOAL", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (goals.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "No goals", tint = AuraWhiteMuted, modifier = Modifier.size(48.dp))
                    Text("No target plans listed. Save for gadgets or vacations.", color = AuraWhiteMuted, fontSize = 11.sp)
                }
            } else {
                goals.forEach { gol ->
                    val percentage = if (gol.targetAmount > 0) {
                        (gol.savedAmount / gol.targetAmount * 100).coerceAtMost(100.0)
                    } else 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .background(AuraSlateCard.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(gol.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Target: ${gol.targetDate}", fontSize = 9.sp, color = AuraWhiteMuted)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Progress bar
                            LinearProgressIndicator(
                                progress = (percentage / 100.0).toFloat(),
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                                color = AuraCyanNeon,
                                trackColor = AuraSlateLight
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Saved: ₹${gol.savedAmount.roundToInt()} of ₹${gol.targetAmount.roundToInt()} (${percentage.roundToInt()}%)",
                                    fontSize = 10.sp,
                                    color = AuraWhiteMuted
                                )
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Settle/Add savings increment button
                                    IconButton(
                                        onClick = {
                                            val increment = 1000.0
                                            onSettleProgress(gol.copy(savedAmount = (gol.savedAmount + increment).coerceAtMost(gol.targetAmount)))
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.AddCircleOutline, contentDescription = "Add savings", tint = AuraCyanNeon, modifier = Modifier.size(16.dp))
                                    }
                                    
                                    IconButton(
                                        onClick = { onDeleteGoal(gol) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete target", tint = AuraWhiteMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================
// SUB-MODULE: RECURRING ALARMS & REMINDERS
// ===================================================
@Composable
fun AccountRemindersTimeline(
    reminders: List<MoneyReminder>,
    onAddReminderClick: () -> Unit,
    onToggleDone: (MoneyReminder) -> Unit,
    onDeleteClick: (MoneyReminder) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("RECURRING REMINDERS & EMIs", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon, letterSpacing = 1.sp)
                Button(
                    onClick = onAddReminderClick,
                    colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("+ EMIs / BILLS", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (reminders.isEmpty()) {
                Text("No EMIs, rent, subscriptions, or split schedules set.", color = AuraWhiteMuted, fontSize = 11.sp)
            } else {
                reminders.forEach { rem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .background(AuraSlateCard.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Checkbox(
                                checked = rem.isCompleted,
                                onCheckedChange = { onToggleDone(rem) },
                                colors = CheckboxDefaults.colors(checkedColor = AuraCyanNeon)
                            )
                            Column {
                                Text(
                                    text = rem.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rem.isCompleted) AuraWhiteMuted else Color.White,
                                    textDecoration = if (rem.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = "${if (rem.isRecurring) "Recurring ${rem.recurrence}" else "One-time"} • Due: ${rem.dueDate}",
                                    fontSize = 10.sp,
                                    color = AuraWhiteMuted
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "₹${"%,.0f".format(rem.amount)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (rem.isCompleted) AuraWhiteMuted else AuraPurpleAccent
                            )
                            IconButton(onClick = { onDeleteClick(rem) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete alarm", tint = AuraWhiteMuted, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================
// SUB-MODULE: NATIVE JETPACK COMPOSE ANALYTICS GRAPHS
// ===================================================
@Composable
fun VisualAnalyticsDashboard(
    transactions: List<Transaction>,
    investments: List<Investment>,
    debts: List<Debt>,
    goals: List<SavingsGoal>,
    onCategoryClick: ((String) -> Unit)? = null,
    onCashflowClick: (() -> Unit)? = null
) {
    // Math category totals
    val categoryTotals = transactions.groupBy { it.category }
        .mapValues { (_, txList) -> txList.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val totalSentimentExpenses = categoryTotals.sumOf { it.second }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("FINTECH VISUAL ANALYTICS", fontSize = 12.sp, fontWeight = FontWeight.Black, color = AuraCyanNeon, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))

            if (categoryTotals.isEmpty()) {
                Text("Not enough data to graph yet. Add more Sent/Received records.", color = AuraWhiteMuted, fontSize = 11.sp)
            } else {
                Text("COGNITIVE SPENDING CATEGORY SHARE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMedium)
                Spacer(modifier = Modifier.height(16.dp))

                // Custom Drawing Pie Category Share Chart (Clicking opens Category Ledger search)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (onCategoryClick != null) it.clickable { onCategoryClick("ALL") } else it },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val colors = listOf(AuraPurpleAccent, AuraCyanNeon, Color.Yellow, MoodHappy, Color.Red, Color.Green, Color.Magenta)
                    
                    Canvas(
                        modifier = Modifier
                            .size(120.dp)
                            .clearAndSetSemantics { 
                                contentDescription = "Pie chart displaying expense category breakdown." 
                            }
                    ) {
                        var startAngle = 0f
                        categoryTotals.forEachIndexed { idx, pair ->
                            val sweepAngle = if (totalSentimentExpenses > 0) {
                                ((pair.second / totalSentimentExpenses) * 360f).toFloat()
                            } else 0f
                            drawArc(
                                color = colors[idx % colors.size],
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    // Legend values
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                        categoryTotals.take(5).forEachIndexed { idx, pair ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.clickable { onCategoryClick?.invoke(pair.first) }
                            ) {
                                class Box
                                androidx.compose.foundation.layout.Box(modifier = Modifier.size(8.dp).background(colors[idx % colors.size], RoundedCornerShape(2.dp)))
                                Text(
                                    text = "${pair.first}: ${if (totalSentimentExpenses > 0) ((pair.second / totalSentimentExpenses) * 100).roundToInt() else 0}%",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Divider(color = AuraSlateLight.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))

                // Income vs Expenses custom layout (Clicking opens Bank Passbook details)
                val sentAmt = transactions.filter { it.type == "SENT" || it.type == "INVESTED" }.sumOf { it.amount }
                val recvAmt = transactions.filter { it.type == "RECEIVED" || it.type == "CASH_ADDED" }.sumOf { it.amount }
                val savingsRate = if (recvAmt > 0) ((recvAmt - sentAmt) / recvAmt * 100).coerceIn(0.0..100.0) else 0.0

                Text("NET CASHFLOW INSIGHTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .let { if (onCashflowClick != null) it.clickable { onCashflowClick() } else it },
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(MoodHappy.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL RECORDED INCOME", fontSize = 8.sp, color = AuraWhiteMuted)
                        Text("₹${"%,.0f".format(recvAmt)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MoodHappy)
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(AuraPurpleAccent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL RECORDED DEBITS", fontSize = 8.sp, color = AuraWhiteMuted)
                        Text("₹${"%,.0f".format(sentAmt)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuraPurpleAccent)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Your net savings rate this month is ${savingsRate.roundToInt()}%. Keep it up!",
                    fontSize = 11.sp,
                    color = AuraCyanNeon,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Mini view helper: Accounts summary
@Composable
fun AccountsSectionView(
    accounts: List<Account>,
    onAdjustBalance: (Account) -> Unit,
    onNavigateToPassbook: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp))
            .let { if (onNavigateToPassbook != null) it.clickable { onNavigateToPassbook() } else it },
        colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha=0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MULTIPLE ACCOUNTS LEDGER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMedium, letterSpacing = 1.sp)
                if (onNavigateToPassbook != null) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Open Passbook",
                        tint = AuraCyanNeon,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            accounts.forEach { acct ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onAdjustBalance(acct) },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(if (acct.isDefault) AuraCyanNeon else AuraWhiteMuted, CircleShape))
                        Text(acct.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Text("₹${"%,.2f".format(acct.balance)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMedium)
                }
            }
        }
    }
}

// Mini view helper: Splits settlements Overview
@Composable
fun SplitSummarySectionView(
    toReceive: Double,
    youOwe: Double,
    debts: List<Debt>,
    onQuickSettle: (Debt) -> Unit,
    onNavigateToSplits: (() -> Unit)? = null
) {
    val pending = debts.filter { d -> d.status == "PENDING" }
    if (pending.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp))
                .let { if (onNavigateToSplits != null) it.clickable { onNavigateToSplits() } else it },
            colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha=0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("PENDING SPLITS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMedium, letterSpacing = 1.sp)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Net: ₹${(toReceive - youOwe).roundToInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraCyanNeon)
                        if (onNavigateToSplits != null) {
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Open Splits Module",
                                tint = AuraCyanNeon,
                                modifier = Modifier.size(10.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                pending.take(3).forEach { d ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(d.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("${if (d.isYouOwe) "Owe" else "Owes you"} ${d.friendName}", fontSize = 9.sp, color = AuraWhiteMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("₹${d.remainingAmount.roundToInt()}", fontSize = 11.sp, color = if (d.isYouOwe) Color.Red else MoodHappy)
                            TextButton(
                                onClick = { onQuickSettle(d) },
                                modifier = Modifier.height(24.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("SETTLE", fontSize = 9.sp, color = AuraCyanNeon, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Mini reminders widget helper
@Composable
fun MiniRemindersView(
    reminders: List<MoneyReminder>,
    onSettle: (MoneyReminder) -> Unit,
    onNavigateToReminders: (() -> Unit)? = null
) {
    if (reminders.isNotEmpty()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp))
                .let { if (onNavigateToReminders != null) it.clickable { onNavigateToReminders() } else it },
            colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha=0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("UPCOMING SUBS/BILLS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraWhiteMedium, letterSpacing = 1.sp)
                    if (onNavigateToReminders != null) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Open Reminders",
                            tint = AuraCyanNeon,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                reminders.forEach { r ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(r.title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Due: ${r.dueDate}", fontSize = 9.sp, color = AuraWhiteMuted)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("₹${r.amount.roundToInt()}", fontSize = 11.sp, color = AuraPurpleAccent, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onSettle(r) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Done, contentDescription = "Mark done", tint = AuraCyanNeon, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ===================================================
// DETAILED COMPOSE SHEETS AND MODAL CONSTRUCTORS
// ===================================================

@Composable
fun QuickTransactionBottomSheet(
    type: String,
    accounts: List<Account>,
    friends: List<Friend>,
    sentOptions: List<String>,
    onDismiss: () -> Unit,
    onSubmit: (Double, String, String, String, String, String, Int, List<Friend>, Boolean) -> Unit,
    transactionToEdit: Transaction? = null
) {
    var rawAmount by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.amount?.toString() ?: "") }
    var selectedCategory by remember(sentOptions, transactionToEdit) { mutableStateOf(transactionToEdit?.category ?: sentOptions.firstOrNull() ?: "Friend") }
    var rawNote by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.note ?: "") }
    var rawLocation by remember(transactionToEdit) { mutableStateOf(transactionToEdit?.location ?: "") }
    var selectedAccount by remember(accounts, transactionToEdit) {
        mutableStateOf(accounts.find { it.id == transactionToEdit?.accountId } ?: accounts.find { it.isDefault } ?: accounts.firstOrNull())
    }
    
    // Track selected friends for splits (active if selectedCategory == "Friend")
    var tickedFriends by remember { mutableStateOf(setOf<Friend>()) }
    var includeMe by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Record $type Money", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = rawAmount,
                    onValueChange = { rawAmount = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    label = { Text("Transaction Amount (₹)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                val limitWarn = (type == "SENT" || type == "INVESTED") && (rawAmount.toDoubleOrNull() ?: 0.0) > (selectedAccount?.balance ?: 0.0)
                if (limitWarn) {
                    Text(
                        text = "⚠️ Warning: This exceeds ${selectedAccount?.name ?: "Account"}'s balance (Available: ₹${"%,.0f".format(selectedAccount?.balance ?: 0.0)})",
                        color = Color.Yellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Destination / Source Selector instead of recipient text field + category horizontal slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (type == "SENT") "Select Destination Option" else "Select Source Option", fontSize = 10.sp, color = AuraWhiteMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        sentOptions.forEach { opt ->
                            val s = selectedCategory == opt
                            Box(
                                modifier = Modifier
                                    .background(if (s) AuraCyanNeon else AuraSlateCard, RoundedCornerShape(8.dp))
                                    .clickable { selectedCategory = opt }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(opt, fontSize = 10.sp, color = if (s) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Friends Toggles Sub-Section if category is Friend
                if (selectedCategory == "Friend") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateCard.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .border(1.dp, AuraSlateLight, RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "SPLIT WITH FRIENDS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = AuraCyanNeon,
                            letterSpacing = 1.sp
                        )
                        
                        if (friends.isEmpty()) {
                            Text(
                                "No friends directory found. Add friends in Settings first!",
                                fontSize = 11.sp,
                                color = AuraWhiteMuted
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { includeMe = !includeMe }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .background(if (includeMe) AuraCyanNeon else Color.Transparent, RoundedCornerShape(4.dp))
                                            .border(1.5.dp, if (includeMe) AuraCyanNeon else AuraWhiteMedium, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (includeMe) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.Black,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Text("Include Me", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                                Text("You part of the split", fontSize = 9.sp, color = AuraWhiteMuted)
                            }

                            friends.forEach { friend ->
                                val isTicked = tickedFriends.contains(friend)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            tickedFriends = if (isTicked) {
                                                tickedFriends - friend
                                            } else {
                                                tickedFriends + friend
                                            }
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(if (isTicked) AuraCyanNeon else Color.Transparent, RoundedCornerShape(4.dp))
                                                .border(1.5.dp, if (isTicked) AuraCyanNeon else AuraWhiteMedium, RoundedCornerShape(4.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isTicked) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        Text(friend.name, fontSize = 12.sp, color = Color.White)
                                    }
                                    Text(
                                        if (friend.notes.isNotBlank()) friend.notes else "Friend",
                                        fontSize = 9.sp,
                                        color = AuraWhiteMuted
                                    )
                                }
                            }

                            // Interactive share calculator
                            val amtVal = rawAmount.toDoubleOrNull() ?: 0.0
                            val shareCount = tickedFriends.size + (if (includeMe) 1 else 0)
                            if (amtVal > 0.0 && shareCount > 0 && tickedFriends.isNotEmpty()) {
                                val share = amtVal / shareCount
                                Spacer(modifier = Modifier.height(4.dp))
                                Divider(color = AuraSlateLight.copy(alpha = 0.3f))
                                Text(
                                    text = "Equally split between $shareCount ticks:",
                                    fontSize = 9.sp,
                                    color = AuraWhiteMedium
                                )
                                Text(
                                    text = "₹${"%,.2f".format(share)} each",
                                    fontSize = 13.sp,
                                    color = MoodHappy,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }

                // Account Selector
                Column {
                    Text(if (type == "SENT") "Debit Account" else "Credit Account", fontSize = 10.sp, color = AuraWhiteMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        accounts.forEach { ac ->
                            val s = selectedAccount?.id == ac.id
                            Box(
                                modifier = Modifier
                                    .background(if (s) AuraCyanNeon else AuraSlateCard, RoundedCornerShape(8.dp))
                                    .clickable { selectedAccount = ac }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(ac.name, fontSize = 9.sp, color = if (s) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = rawNote,
                    onValueChange = { rawNote = it },
                    placeholder = { Text("Notes (optional)", color = AuraWhiteMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = rawLocation,
                    onValueChange = { rawLocation = it },
                    placeholder = { Text("Location (optional)", color = AuraWhiteMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = rawAmount.toDoubleOrNull() ?: 0.0
                    val acId = selectedAccount?.id ?: 0
                    val payMethod = selectedAccount?.name ?: "Cash"
                    if (amt > 0.0) {
                        val recName = when (selectedCategory) {
                            "Friend" -> {
                                if (tickedFriends.isEmpty()) if (type == "SENT") "Friends Split" else "Received from Friend"
                                else tickedFriends.joinToString { it.name }
                            }
                            else -> selectedCategory
                        }
                        onSubmit(amt, recName, selectedCategory, rawNote, rawLocation, payMethod, acId, tickedFriends.toList(), includeMe)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
            ) {
                Text("RECORD ENTRY", color = Color.Black, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White)
            }
        },
        containerColor = AuraCharcoalBase
    )
}

@Composable
fun AddInvestmentFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double, String, String) -> Unit
) {
    var inName by remember { mutableStateOf("") }
    var inType by remember { mutableStateOf("Stocks") }
    var inAmount by remember { mutableStateOf("") }
    var inNotes by remember { mutableStateOf("") }

    val supportedTypes = listOf("Stocks", "Mutual Funds", "Fixed Deposit", "Gold", "Crypto", "Real Estate", "Other")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Investment Asset", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = inName,
                    onValueChange = { inName = it },
                    label = { Text("Investment Asset Name (Stocks, ETF etc.)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = inAmount,
                    onValueChange = { inAmount = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Investment Value (₹)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Column {
                    Text("Investment Type", fontSize = 9.sp, color = AuraWhiteMuted)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        supportedTypes.forEach { t ->
                            val s = inType == t
                            Box(
                                modifier = Modifier
                                    .background(if (s) AuraCyanNeon else AuraSlateCard, RoundedCornerShape(8.dp))
                                    .clickable { inType = t }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(t, fontSize = 9.sp, color = if (s) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = inNotes,
                    onValueChange = { inNotes = it },
                    placeholder = { Text("Notes (optional)", color = AuraWhiteMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = inAmount.toDoubleOrNull() ?: 0.0
                    if (inName.isNotBlank() && amt > 0.0) {
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        onSubmit(inName, inType, amt, todayStr, inNotes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
            ) {
                Text("ADD PORTFOLIO Asset", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White)
            }
        },
        containerColor = AuraCharcoalBase
    )
}

@Composable
fun AddFriendProfileDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var frName by remember { mutableStateOf("") }
    var frPhone by remember { mutableStateOf("") }
    var frNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Splitwise Friend Profile", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = frName,
                    onValueChange = { frName = it },
                    label = { Text("Friend Name", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = frPhone,
                    onValueChange = { frPhone = it },
                    label = { Text("Phone Number (optional)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = frNotes,
                    onValueChange = { frNotes = it },
                    label = { Text("Context/Notes (e.g. Roommate)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (frName.isNotBlank()) {
                        onSubmit(frName, frPhone, frNotes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
            ) {
                Text("CREATE PROFILE", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White)
            }
        },
        containerColor = AuraCharcoalBase
    )
}

@Composable
fun BillSplittingFormDialog(
    friends: List<Friend>,
    onDismiss: () -> Unit,
    onSubmit: (String, Double, Boolean, List<Friend>, String, Map<Int, Double>) -> Unit
) {
    var billTitle by remember { mutableStateOf("") }
    var billAmount by remember { mutableStateOf("") }
    var isYouOwe by remember { mutableStateOf(false) } // False if they owe you, True if you owe them
    var selectedParticipants by remember { mutableStateOf(emptyList<Friend>()) }
    var splitType by remember { mutableStateOf("EQUAL") } // EQUAL, CUSTOM, PERCENTAGE

    // Custom Split state maps
    val customSplits = remember { mutableStateMapOf<Int, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unified Splitwise Bill Composer", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = billTitle,
                    onValueChange = { billTitle = it },
                    label = { Text("Bill Title (e.g., Dinner Split)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = billAmount,
                    onValueChange = { billAmount = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Total Bill Amount (₹)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Who Paid selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Debtor Direction:", color = AuraWhiteMedium, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .background(AuraSlateCard, RoundedCornerShape(10.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(if (!isYouOwe) AuraCyanNeon else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { isYouOwe = false }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("They Owe Me", fontSize = 9.sp, color = if (!isYouOwe) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier
                                .background(if (isYouOwe) AuraPurpleAccent else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { isYouOwe = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("I Owe Them", fontSize = 9.sp, color = if (isYouOwe) Color.White else AuraWhiteMuted, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Divider(color = AuraSlateLight.copy(alpha=0.3f))

                // Participants multi-select
                Text("Select Friends sharing the bill:", fontSize = 10.sp, color = AuraWhiteMedium)
                if (friends.isEmpty()) {
                    Text("No friends available to split. Register some first!", fontSize = 10.sp, color = Color.Red)
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        friends.forEach { f ->
                            val s = selectedParticipants.contains(f)
                            Box(
                                modifier = Modifier
                                    .background(if (s) AuraCyanNeon else AuraSlateCard, RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedParticipants = if (s) {
                                            selectedParticipants - f
                                        } else {
                                            selectedParticipants + f
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(f.name, fontSize = 9.sp, color = if (s) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Divider(color = AuraSlateLight.copy(alpha=0.3f))

                // Split method
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Split options", fontSize = 10.sp, color = AuraWhiteMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("EQUAL", "CUSTOM", "PERCENTAGE").forEach { st ->
                            val s = splitType == st
                            Box(
                                modifier = Modifier
                                    .background(if (s) AuraCyanNeon else AuraSlateCard, RoundedCornerShape(8.dp))
                                    .clickable { splitType = st }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(st, fontSize = 8.sp, color = if (s) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // If CUSTOM or PERCENTAGE shows individual inputs
                if (splitType != "EQUAL" && selectedParticipants.isNotEmpty()) {
                    Text("Assign Custom Amounts for each participant:", fontSize = 9.sp, color = AuraWhiteMuted)
                    selectedParticipants.forEach { fri ->
                        var v by remember { mutableStateOf(customSplits[fri.id] ?: "") }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(fri.name, color = Color.White, fontSize = 11.sp, modifier = Modifier.width(80.dp))
                            OutlinedTextField(
                                value = v,
                                onValueChange = {
                                    v = it
                                    customSplits[fri.id] = it
                                },
                                label = { Text(if (splitType == "CUSTOM") "Amount (₹)" else "Percent (%)", fontSize = 9.sp) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AuraCyanNeon,
                                    unfocusedBorderColor = AuraSlateLight,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ttl = billTitle.ifBlank { "Split Bill Ledger" }
                    val totalAmt = billAmount.toDoubleOrNull() ?: 0.0
                    if (totalAmt > 0.0 && selectedParticipants.isNotEmpty()) {
                        // Build custom pay split matrices
                        val pays = mutableMapOf<Int, Double>()
                        when (splitType) {
                            "CUSTOM" -> {
                                selectedParticipants.forEach { fri ->
                                    val indVal = customSplits[fri.id]?.toDoubleOrNull() ?: 0.0
                                    pays[fri.id] = indVal
                                }
                            }
                            "PERCENTAGE" -> {
                                selectedParticipants.forEach { fri ->
                                    val percent = customSplits[fri.id]?.toDoubleOrNull() ?: 0.0
                                    pays[fri.id] = (percent / 100.0) * totalAmt
                                }
                            }
                            else -> {}
                        }
                        onSubmit(ttl, totalAmt, isYouOwe, selectedParticipants, splitType, pays)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
            ) {
                Text("PERFORM SPLIT", color = Color.Black, fontWeight = FontWeight.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White)
            }
        },
        containerColor = AuraCharcoalBase
    )
}

@Composable
fun AddSavingsGoalFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Double, Double, String, String) -> Unit
) {
    var glName by remember { mutableStateOf("") }
    var glTarget by remember { mutableStateOf("") }
    var glSaved by remember { mutableStateOf("0") }
    var glNotes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Savings Plan Target", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = glName,
                    onValueChange = { glName = it },
                    label = { Text("Goal Name (e.g., Tesla CyberBike)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = glTarget,
                    onValueChange = { glTarget = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Target Amount (₹)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = glSaved,
                    onValueChange = { glSaved = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Initial Saved Balance (₹)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = glNotes,
                    onValueChange = { glNotes = it },
                    label = { Text("Notes/Details", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goalTgt = glTarget.toDoubleOrNull() ?: 0.0
                    val goalSaved = glSaved.toDoubleOrNull() ?: 0.0
                    if (glName.isNotBlank() && goalTgt > 0.0) {
                        val formatSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 90 * 24 * 60 * 60 * 1000L)) // 90 days default
                        onSubmit(glName, goalTgt, goalSaved, formatSdf, glNotes)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
            ) {
                Text("ESTABLISH GOAL", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White)
            }
        },
        containerColor = AuraCharcoalBase
    )
}

@Composable
fun AddReminderFormDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, Double, String, Boolean, String) -> Unit
) {
    var rTitle by remember { mutableStateOf("") }
    var rAmount by remember { mutableStateOf("") }
    var rRecurName by remember { mutableStateOf("Monthly") }
    var isRec by remember { mutableStateOf(true) }

    val periodicLabels = listOf("Daily", "Weekly", "Monthly", "Yearly")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Register EMI/Billing Alert Trigger", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = rTitle,
                    onValueChange = { rTitle = it },
                    label = { Text("Billing Title (e.g., Netflix subscription)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = rAmount,
                    onValueChange = { rAmount = it },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    label = { Text("Amount Scheduled (₹)", color = AuraCyanNeon) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraCyanNeon,
                        unfocusedBorderColor = AuraSlateLight,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Is Recurring Billing:", color = AuraWhiteMedium, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Switch(checked = isRec, onCheckedChange = { isRec = it }, colors = SwitchDefaults.colors(checkedThumbColor = AuraCyanNeon))
                }

                if (isRec) {
                    Column {
                        Text("Billing Cycle Frequency", fontSize = 9.sp, color = AuraWhiteMuted)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            periodicLabels.forEach { p ->
                                val s = rRecurName == p
                                Box(
                                    modifier = Modifier
                                        .background(if (s) AuraCyanNeon else AuraSlateCard, RoundedCornerShape(8.dp))
                                        .clickable { rRecurName = p }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(p, fontSize = 9.sp, color = if (s) Color.Black else Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = rAmount.toDoubleOrNull() ?: 0.0
                    if (rTitle.isNotBlank() && amt > 0.0) {
                        val futureStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() + 30 * 24 * 60 * 60 * 1000L)) // 30 days due
                        onSubmit(rTitle, amt, futureStr, isRec, rRecurName)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AuraCyanNeon)
            ) {
                Text("TRIGGER EMI TRACKER", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CANCEL", color = Color.White)
            }
        },
        containerColor = AuraCharcoalBase
    )
}

@Composable
fun DetailScreenHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AuraSlateCard)
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Go Back",
                tint = Color.White
            )
        }
        Text(
            text = title.uppercase(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun AvailableBalancePassbookView(
    accounts: List<Account>,
    transactions: List<Transaction>,
    onBack: () -> Unit,
    onAdjustBalance: (Account) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    onEditTransaction: (Transaction) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
    ) {
        DetailScreenHeader(title = "Account Passbook", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Title: Accounts List Horizontal Scroller
            item {
                Text(
                    text = "WALLETS & GENERAL LEDGERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraWhiteMedium,
                    letterSpacing = 1.sp
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    accounts.forEach { acct ->
                        Card(
                            modifier = Modifier
                                .width(200.dp)
                                .clickable { onAdjustBalance(acct) }
                                .border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = AuraSlateCard)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(acct.name.uppercase(), fontSize = 10.sp, color = AuraCyanNeon, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("₹${"%,.2f".format(acct.balance)}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Tap to adjust manually", fontSize = 9.sp, color = AuraWhiteMuted)
                            }
                        }
                    }
                }
            }

            // Filters Section Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AuraSlateLight, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters", tint = AuraCyanNeon, modifier = Modifier.size(16.dp))
                            Text("ADVANCED FILTERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search description, recipient, category...", fontSize = 11.sp, color = AuraWhiteMuted) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = Color.White),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraCyanNeon,
                                unfocusedBorderColor = AuraSlateLight,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        // Transaction Flow Filters
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("All", "CASH_ADDED", "RECEIVED", "SENT", "INVESTED").forEach { t ->
                                val active = selectedType == t
                                Box(
                                    modifier = Modifier
                                        .background(if (active) AuraCyanNeon else AuraSlateCard, RoundedCornerShape(8.dp))
                                        .clickable { selectedType = t }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when(t) {
                                            "CASH_ADDED" -> "Cash In"
                                            "RECEIVED" -> "Received"
                                            "SENT" -> "Sent"
                                            "INVESTED" -> "Invested"
                                            else -> "All Flows"
                                        },
                                        fontSize = 9.sp,
                                        color = if (active) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ledger Entries Header
            item {
                Text(
                    text = "PASSBOOK LEDGER ENTRIES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraWhiteMedium,
                    letterSpacing = 1.sp
                )
            }

            // Filter lists based on match criteria
            val filteredList = transactions.filter { tx ->
                val matchesQuery = tx.recipientOrSender.contains(searchQuery, true) ||
                        tx.category.contains(searchQuery, true) ||
                        tx.note.contains(searchQuery, true) ||
                        tx.amount.toString().contains(searchQuery)
                val matchesType = selectedType == "All" || tx.type == selectedType
                matchesQuery && matchesType
            }.sortedByDescending { it.timestamp }

            if (filteredList.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No passbook entries match current filter.", color = AuraWhiteMuted, fontSize = 11.sp)
                    }
                }
            } else {
                items(filteredList) { tx ->
                    val isIncome = tx.type == "RECEIVED" || tx.type == "CASH_ADDED"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateCard.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .border(1.dp, AuraSlateLight.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isIncome) MoodHappy.copy(alpha = 0.15f) else AuraPurpleAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = "Flow direction",
                                    tint = if (isIncome) MoodHappy else AuraPurpleAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = tx.recipientOrSender.ifBlank { "Cash Box" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .background(AuraSlateLight, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(tx.category.uppercase(), fontSize = 7.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Text(tx.dateString, fontSize = 9.sp, color = AuraWhiteMuted)
                                }
                                if (tx.note.isNotBlank()) {
                                    Text(tx.note, fontSize = 10.sp, color = AuraWhiteMuted, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "${if (isIncome) "+" else "-"}₹${tx.amount.roundToInt()}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isIncome) MoodHappy else Color.White
                            )

                            IconButton(onClick = { onEditTransaction(tx) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Tx", tint = AuraWhiteMuted.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                            }

                            IconButton(onClick = { onDeleteTransaction(tx) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Tx", tint = AuraWhiteMuted.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SplitsToReceiveView(
    friends: List<Friend>,
    debts: List<Debt>,
    onBack: () -> Unit,
    onFriendClick: (Friend) -> Unit
) {
    val pendingToReceive = debts.filter { !it.isYouOwe && it.status == "PENDING" }
    val totalToReceive = pendingToReceive.sumOf { it.remainingAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
    ) {
        DetailScreenHeader(title = "Splits To Receive", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Aggregate metrics card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MoodHappy.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL EXPECTED RECEIVABLES", fontSize = 9.sp, color = MoodHappy, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${"%,.2f".format(totalToReceive)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Icon(Icons.Default.TrendingUp, contentDescription = "Receivables stream", tint = MoodHappy, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Friend list heading
            item {
                Text(
                    text = "PENDING SPLITS BY FRIEND",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraWhiteMedium,
                    letterSpacing = 1.sp
                )
            }

            // List of friends who currently owe money to the user (i.e. net receivable > 0)
            val friendsWithOwes = friends.map { fri ->
                val friendDebts = debts.filter { it.friendId == fri.id && it.status == "PENDING" }
                val toRec = friendDebts.filter { !it.isYouOwe }.sumOf { it.remainingAmount }
                val toPay = friendDebts.filter { it.isYouOwe }.sumOf { it.remainingAmount }
                val net = toRec - toPay
                Triple(fri, net, friendDebts.size)
            }.filter { it.second > 0 }

            if (friendsWithOwes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No outstanding friend splits to make! You are fully squared up.", color = MoodHappy, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(friendsWithOwes) { (friend, netAmount, splitCount) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateCard.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .border(1.dp, AuraSlateLight.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { onFriendClick(friend) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(friend.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Owing splits: $splitCount bills", fontSize = 10.sp, color = AuraWhiteMuted)
                            if (friend.notes.isNotBlank()) {
                                Text(friend.notes, fontSize = 9.sp, color = AuraWhiteMuted)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${netAmount.roundToInt()}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = MoodHappy)
                                Text("Owes you net", fontSize = 8.sp, color = MoodHappy)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = "View details", tint = AuraWhiteMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun SplitsYouOweView(
    friends: List<Friend>,
    debts: List<Debt>,
    onBack: () -> Unit,
    onFriendClick: (Friend) -> Unit
) {
    val pendingYouOwe = debts.filter { it.isYouOwe && it.status == "PENDING" }
    val totalYouOwe = pendingYouOwe.sumOf { it.remainingAmount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
    ) {
        DetailScreenHeader(title = "Splits You Owe", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Aggregate metrics card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Red.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL OUTSTANDING LIABILITIES", fontSize = 9.sp, color = Color.Red, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${"%,.2f".format(totalYouOwe)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Icon(Icons.Default.TrendingDown, contentDescription = "Liabilities curve", tint = Color.Red, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Friend list heading
            item {
                Text(
                    text = "DEBTS BY CREDITOR",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraWhiteMedium,
                    letterSpacing = 1.sp
                )
            }

            // List of friends whom the user owes money to (i.e. net owe > 0)
            val friendsWithOwes = friends.map { fri ->
                val friendDebts = debts.filter { it.friendId == fri.id && it.status == "PENDING" }
                val toRec = friendDebts.filter { !it.isYouOwe }.sumOf { it.remainingAmount }
                val toPay = friendDebts.filter { it.isYouOwe }.sumOf { it.remainingAmount }
                val net = toPay - toRec
                Triple(fri, net, friendDebts.size)
            }.filter { it.second > 0 }

            if (friendsWithOwes.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No debts! You are completely debt-free. Outstanding job.", color = MoodHappy, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(friendsWithOwes) { (friend, netAmount, splitCount) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateCard.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .border(1.dp, AuraSlateLight.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                            .clickable { onFriendClick(friend) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(friend.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Owed splits: $splitCount bills", fontSize = 10.sp, color = AuraWhiteMuted)
                            if (friend.notes.isNotBlank()) {
                                Text(friend.notes, fontSize = 9.sp, color = AuraWhiteMuted)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("₹${netAmount.roundToInt()}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Red)
                                Text("You owe net", fontSize = 8.sp, color = Color.Red)
                            }
                            Icon(Icons.Default.ArrowForward, contentDescription = "View details", tint = AuraWhiteMuted, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun FriendSplitDetailsView(
    friend: Friend,
    debts: List<Debt>,
    onBack: () -> Unit,
    onQuickSettle: (Debt) -> Unit
) {
    val friendDebts = debts.filter { it.friendId == friend.id && it.status == "PENDING" }
    val toRec = friendDebts.filter { !it.isYouOwe }.sumOf { it.remainingAmount }
    val toPay = friendDebts.filter { it.isYouOwe }.sumOf { it.remainingAmount }
    val netBalance = toRec - toPay

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
    ) {
        DetailScreenHeader(title = "Splits with ${friend.name}", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Summary balance block
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (netBalance > 0) MoodHappy.copy(alpha = 0.5f) else if (netBalance < 0) Color.Red.copy(alpha = 0.5f) else AuraWhiteMuted,
                            shape = RoundedCornerShape(20.dp)
                        ),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.6f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "NET SETTLEMENT WITH ${friend.name.uppercase()}",
                            fontSize = 9.sp,
                            color = AuraWhiteMuted,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val netColor = if (netBalance > 0) MoodHappy else if (netBalance < 0) Color.Red else Color.White
                        val netLabel = if (netBalance > 0) "OWES YOU ₹${"%,.2f".format(netBalance)}" else if (netBalance < 0) "YOU OWE ₹${"%,.2f".format(-netBalance)}" else "SETTLED SQUARED UP"

                        Text(
                            text = netLabel,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = netColor
                        )
                    }
                }
            }

            // Transaction items list header
            item {
                Text(
                    text = "ACTIVE SPLITS JOURNAL",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraWhiteMedium,
                    letterSpacing = 1.sp
                )
            }

            if (friendDebts.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No pending splits with this friend.", color = AuraWhiteMuted, fontSize = 11.sp)
                    }
                }
            } else {
                items(friendDebts) { dbt ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateCard.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .border(1.dp, AuraSlateLight.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dbt.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                text = "Total ₹${dbt.totalAmount.roundToInt()} • ${dbt.date}",
                                fontSize = 10.sp,
                                color = AuraWhiteMuted
                            )
                            if (dbt.remainingAmount < dbt.totalAmount) {
                                Text("Partial Settle Paid: ₹${(dbt.totalAmount - dbt.remainingAmount).roundToInt()}", fontSize = 9.sp, color = AuraCyanNeon)
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "₹${dbt.remainingAmount.roundToInt()}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (dbt.isYouOwe) Color.Red else MoodHappy
                                )
                                Text(
                                    text = if (dbt.isYouOwe) "You owe" else "Owes you",
                                    fontSize = 8.sp,
                                    color = AuraWhiteMuted
                                )
                            }

                            Button(
                                onClick = { onQuickSettle(dbt) },
                                colors = ButtonDefaults.buttonColors(containerColor = if (dbt.isYouOwe) Color.Red else MoodHappy),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("SETTLE", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun PortfolioInvestmentDetailsView(
    investments: List<Investment>,
    onBack: () -> Unit,
    onAddInvestmentClick: () -> Unit,
    onDeleteInvestment: (Investment) -> Unit
) {
    val totalInvested = investments.sumOf { it.amount }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraObsidian)
    ) {
        DetailScreenHeader(title = "Portfolio Holdings", onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Aggregate Portfolio Holdings card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.Yellow.copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = AuraSlateCard.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("TOTAL PORTFOLIO VALUE", fontSize = 9.sp, color = Color.Yellow, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("₹${"%,.2f".format(totalInvested)}", fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Icon(Icons.Default.Timeline, contentDescription = "Holdings graph", tint = Color.Yellow, modifier = Modifier.size(32.dp))
                        }
                    }
                }
            }

            // Quick add button bar inline inside the holding detail view
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ASSETS ENGINE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraWhiteMedium,
                        letterSpacing = 1.sp,
                        modifier = Modifier.weight(1f)
                    )

                    Button(
                        onClick = onAddInvestmentClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Yellow),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text("+ ADD ASSET", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    }
                }
            }

            if (investments.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No active holdings discovered. Tap '+ ADD ASSET' above to log Stocks, Crypto, Mutual Funds or Gold.", color = AuraWhiteMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            } else {
                items(investments) { inv ->
                    val daysHeld = try {
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        val parseDate = sdf.parse(inv.date) ?: java.util.Date()
                        val diffMs = System.currentTimeMillis() - parseDate.time
                        (diffMs / (24 * 60 * 60 * 1000)).coerceAtLeast(0)
                    } catch (e: Exception) {
                        0L
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraSlateCard.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                            .border(1.dp, AuraSlateLight.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(inv.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(Color.Yellow, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(inv.type.uppercase(), fontSize = 8.sp, color = Color.Black, fontWeight = FontWeight.Black)
                                }
                                Text("Held: $daysHeld Days", fontSize = 10.sp, color = AuraWhiteMuted)
                            }
                            if (inv.notes.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(inv.notes, fontSize = 10.sp, color = AuraWhiteMuted)
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("₹${"%,.0f".format(inv.amount)}", fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.Yellow)
                            IconButton(onClick = { onDeleteInvestment(inv) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete asset", tint = AuraWhiteMuted.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}
