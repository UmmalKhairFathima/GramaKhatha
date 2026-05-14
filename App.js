import React, { useEffect, useMemo, useState } from "react";
import {
  Alert,
  FlatList,
  KeyboardAvoidingView,
  Modal,
  Platform,
  Pressable,
  SafeAreaView,
  ScrollView,
  StatusBar,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  View
} from "react-native";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Ionicons } from "@expo/vector-icons";

const STORAGE_KEYS = {
  auth: "nsl.auth",
  transactions: "nsl.transactions",
  profile: "nsl.profile",
  settings: "nsl.settings"
};

const emptyProfile = {
  shopName: "",
  ownerName: "",
  ownerPhone: "",
  ownerAddress: ""
};

const defaultSettings = {
  dailyEnabled: false,
  weeklyEnabled: false,
  hour: 20,
  minute: 0
};

export default function App() {
  const [ready, setReady] = useState(false);
  const [auth, setAuth] = useState({ pin: null, isLoggedIn: false });
  const [transactions, setTransactions] = useState([]);
  const [profile, setProfile] = useState(emptyProfile);
  const [settings, setSettings] = useState(defaultSettings);
  const [screen, setScreen] = useState("dashboard");

  useEffect(() => {
    loadState();
  }, []);

  async function loadState() {
    const [savedAuth, savedTransactions, savedProfile, savedSettings] = await Promise.all([
      AsyncStorage.getItem(STORAGE_KEYS.auth),
      AsyncStorage.getItem(STORAGE_KEYS.transactions),
      AsyncStorage.getItem(STORAGE_KEYS.profile),
      AsyncStorage.getItem(STORAGE_KEYS.settings)
    ]);

    if (savedAuth) setAuth(JSON.parse(savedAuth));
    if (savedTransactions) setTransactions(JSON.parse(savedTransactions));
    if (savedProfile) setProfile({ ...emptyProfile, ...JSON.parse(savedProfile) });
    if (savedSettings) setSettings({ ...defaultSettings, ...JSON.parse(savedSettings) });
    setReady(true);
  }

  async function persistAuth(nextAuth) {
    setAuth(nextAuth);
    await AsyncStorage.setItem(STORAGE_KEYS.auth, JSON.stringify(nextAuth));
  }

  async function persistTransactions(nextTransactions) {
    setTransactions(nextTransactions);
    await AsyncStorage.setItem(STORAGE_KEYS.transactions, JSON.stringify(nextTransactions));
  }

  async function persistProfile(nextProfile) {
    setProfile(nextProfile);
    await AsyncStorage.setItem(STORAGE_KEYS.profile, JSON.stringify(nextProfile));
  }

  async function persistSettings(nextSettings) {
    setSettings(nextSettings);
    await AsyncStorage.setItem(STORAGE_KEYS.settings, JSON.stringify(nextSettings));
  }

  if (!ready) {
    return (
      <SafeAreaView style={styles.loadingScreen}>
        <StatusBar barStyle="light-content" />
        <Text style={styles.loadingTitle}>Gramakhata</Text>
      </SafeAreaView>
    );
  }

  if (!auth.isLoggedIn) {
    return <LoginScreen auth={auth} onLogin={persistAuth} />;
  }

  return (
    <SafeAreaView style={styles.appShell}>
      <StatusBar barStyle="light-content" backgroundColor="#172554" />
      {screen === "dashboard" && (
        <DashboardScreen
          auth={auth}
          profile={profile}
          transactions={transactions}
          onTransactionsChange={persistTransactions}
          onNavigate={setScreen}
          onLogout={() => persistAuth({ ...auth, isLoggedIn: false })}
        />
      )}
      {screen === "profile" && (
        <ProfileScreen profile={profile} onSave={persistProfile} onBack={() => setScreen("dashboard")} />
      )}
      {screen === "settings" && (
        <SettingsScreen
          auth={auth}
          settings={settings}
          onSaveSettings={persistSettings}
          onSaveAuth={persistAuth}
          onBack={() => setScreen("dashboard")}
        />
      )}
    </SafeAreaView>
  );
}

function LoginScreen({ auth, onLogin }) {
  const [pin, setPin] = useState("");
  const hasPin = Boolean(auth.pin);

  function submit() {
    if (!/^\d{4}$/.test(pin)) {
      Alert.alert("Invalid PIN", "Enter exactly 4 digits.");
      return;
    }

    if (hasPin && pin !== auth.pin) {
      setPin("");
      Alert.alert("Wrong PIN", "Please try again.");
      return;
    }

    onLogin({ pin, isLoggedIn: true });
  }

  return (
    <SafeAreaView style={styles.loginScreen}>
      <StatusBar barStyle="light-content" backgroundColor="#172554" />
      <KeyboardAvoidingView behavior={Platform.OS === "ios" ? "padding" : undefined} style={styles.loginContent}>
        <View style={styles.brandBadge}>
          <Ionicons name="storefront" size={34} color="#ffffff" />
        </View>
        <Text style={styles.loginTitle}>{hasPin ? "Welcome Back" : "Create PIN"}</Text>
        <Text style={styles.loginSubtitle}>
          {hasPin ? "Enter your PIN to continue" : "Set a 4-digit PIN to secure your ledger"}
        </Text>
        <TextInput
          value={pin}
          onChangeText={(value) => setPin(value.replace(/\D/g, "").slice(0, 4))}
          secureTextEntry
          keyboardType="number-pad"
          maxLength={4}
          placeholder="4-digit PIN"
          placeholderTextColor="#94a3b8"
          style={styles.pinInput}
        />
        <Pressable style={styles.primaryButton} onPress={submit}>
          <Ionicons name={hasPin ? "log-in-outline" : "lock-closed-outline"} size={18} color="#ffffff" />
          <Text style={styles.primaryButtonText}>{hasPin ? "Login" : "Create PIN"}</Text>
        </Pressable>
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

function DashboardScreen({ auth, profile, transactions, onTransactionsChange, onNavigate, onLogout }) {
  const [name, setName] = useState("");
  const [phone, setPhone] = useState("");
  const [amount, setAmount] = useState("");

  const totalAmount = useMemo(
    () => transactions.reduce((sum, item) => sum + Number(item.amount || 0), 0),
    [transactions]
  );

  function addTransaction() {
    const parsedAmount = Number(amount);
    if (!name.trim() || !amount.trim()) {
      Alert.alert("Missing details", "Customer name and amount are required.");
      return;
    }
    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      Alert.alert("Invalid amount", "Enter a valid amount.");
      return;
    }

    const now = new Date();
    const nextTransaction = {
      id: String(now.getTime()),
      customerName: name.trim(),
      phoneNumber: phone.trim(),
      amount: parsedAmount,
      timestamp: now.toLocaleString("en-IN", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
      })
    };

    onTransactionsChange([nextTransaction, ...transactions]);
    setName("");
    setPhone("");
    setAmount("");
  }

  function deleteTransaction(item) {
    Alert.alert("Delete Transaction", `Delete ${item.customerName}'s entry of Rs. ${item.amount.toFixed(2)}?`, [
      { text: "Cancel", style: "cancel" },
      {
        text: "Delete",
        style: "destructive",
        onPress: () => onTransactionsChange(transactions.filter((transaction) => transaction.id !== item.id))
      }
    ]);
  }

  function clearAll() {
    if (transactions.length === 0) {
      Alert.alert("Nothing to clear", "There are no transactions yet.");
      return;
    }

    Alert.alert("Clear All", `Delete all ${transactions.length} transactions?`, [
      { text: "Cancel", style: "cancel" },
      { text: "Clear All", style: "destructive", onPress: () => onTransactionsChange([]) }
    ]);
  }

  function confirmLogout() {
    Alert.alert("Logout", "Are you sure you want to logout?", [
      { text: "Cancel", style: "cancel" },
      { text: "Logout", style: "destructive", onPress: onLogout }
    ]);
  }

  return (
    <View style={styles.flex}>
      <View style={styles.header}>
        <View style={styles.headerTextBlock}>
          <Text style={styles.headerTitle}>{profile.shopName || "Gramakhata"}</Text>
          <Text style={styles.headerDate}>
            {new Date().toLocaleDateString("en-IN", { weekday: "long", day: "2-digit", month: "short", year: "numeric" })}
          </Text>
        </View>
        <View style={styles.headerActions}>
          <IconButton icon="person-outline" onPress={() => onNavigate("profile")} />
          <IconButton icon="settings-outline" onPress={() => onNavigate("settings")} />
          <IconButton icon="log-out-outline" onPress={confirmLogout} />
        </View>
      </View>

      <FlatList
        data={transactions}
        keyExtractor={(item) => item.id}
        contentContainerStyle={styles.dashboardContent}
        ListHeaderComponent={
          <>
            <View style={styles.summaryRow}>
              <SummaryCard label="TRANSACTIONS" value={String(transactions.length)} detail="Total entries" color="#1d4ed8" />
              <SummaryCard label="TOTAL AMOUNT" value={`Rs. ${totalAmount.toFixed(2)}`} detail="Collected so far" color="#15803d" />
            </View>

            <View style={styles.panel}>
              <View style={styles.panelTitleRow}>
                <Ionicons name="add-circle-outline" size={19} color="#172554" />
                <Text style={styles.panelTitle}>Add Transaction</Text>
              </View>
              <View style={styles.formRow}>
                <TextInput
                  value={name}
                  onChangeText={setName}
                  placeholder="Customer Name"
                  placeholderTextColor="#94a3b8"
                  style={[styles.input, styles.formHalfInput]}
                />
                <TextInput
                  value={amount}
                  onChangeText={setAmount}
                  placeholder="Amount Rs."
                  placeholderTextColor="#94a3b8"
                  keyboardType="decimal-pad"
                  style={[styles.input, styles.formHalfInput]}
                />
              </View>
              <TextInput
                value={phone}
                onChangeText={setPhone}
                placeholder="Customer Phone"
                placeholderTextColor="#94a3b8"
                keyboardType="phone-pad"
                style={styles.input}
              />
              <Pressable style={styles.primaryButton} onPress={addTransaction}>
                <Ionicons name="save-outline" size={18} color="#ffffff" />
                <Text style={styles.primaryButtonText}>Save Transaction</Text>
              </Pressable>
            </View>

            <View style={styles.sectionHeader}>
              <Text style={styles.sectionTitle}>Recent Transactions</Text>
              <Pressable onPress={clearAll} hitSlop={10}>
                <Text style={styles.clearText}>Clear All</Text>
              </Pressable>
            </View>
          </>
        }
        ListEmptyComponent={<Text style={styles.emptyText}>No transactions yet.</Text>}
        renderItem={({ item }) => <TransactionItem item={item} onLongPress={() => deleteTransaction(item)} />}
      />
    </View>
  );
}

function ProfileScreen({ profile, onSave, onBack }) {
  const [draft, setDraft] = useState(profile);

  function update(field, value) {
    setDraft((current) => ({ ...current, [field]: value }));
  }

  function save() {
    if (!draft.shopName.trim() || !draft.ownerName.trim()) {
      Alert.alert("Missing details", "Shop name and owner name are required.");
      return;
    }
    onSave({
      shopName: draft.shopName.trim(),
      ownerName: draft.ownerName.trim(),
      ownerPhone: draft.ownerPhone.trim(),
      ownerAddress: draft.ownerAddress.trim()
    });
    onBack();
  }

  return (
    <ScreenFrame title="My Profile" onBack={onBack}>
      <View style={styles.profileHero}>
        <View style={styles.profileAvatar}>
          <Ionicons name="storefront" size={34} color="#172554" />
        </View>
        <Text style={styles.profileHeroTitle}>Shop Profile</Text>
      </View>
      <View style={styles.panel}>
        <LabeledInput label="Shop Name *" value={draft.shopName} onChangeText={(value) => update("shopName", value)} placeholder="e.g. Gramakhata" />
        <LabeledInput label="Owner Name *" value={draft.ownerName} onChangeText={(value) => update("ownerName", value)} placeholder="Your full name" />
        <LabeledInput label="Phone Number" value={draft.ownerPhone} onChangeText={(value) => update("ownerPhone", value)} placeholder="Your phone number" keyboardType="phone-pad" />
        <LabeledInput label="Shop Address" value={draft.ownerAddress} onChangeText={(value) => update("ownerAddress", value)} placeholder="Shop address" multiline />
        <Pressable style={styles.primaryButton} onPress={save}>
          <Ionicons name="checkmark-circle-outline" size={18} color="#ffffff" />
          <Text style={styles.primaryButtonText}>Save Profile</Text>
        </Pressable>
      </View>
    </ScreenFrame>
  );
}

function SettingsScreen({ auth, settings, onSaveSettings, onSaveAuth, onBack }) {
  const [pinModalOpen, setPinModalOpen] = useState(false);
  const [newPin, setNewPin] = useState("");

  function updateSettings(nextPatch) {
    onSaveSettings({ ...settings, ...nextPatch });
  }

  function changePin() {
    if (!/^\d{4}$/.test(newPin)) {
      Alert.alert("Invalid PIN", "PIN must be 4 digits.");
      return;
    }
    onSaveAuth({ ...auth, pin: newPin });
    setNewPin("");
    setPinModalOpen(false);
    Alert.alert("PIN changed", "Your new PIN has been saved.");
  }

  return (
    <ScreenFrame title="Settings" onBack={onBack}>
      <View style={styles.panel}>
        <SettingsTitle icon="notifications-outline" title="Daily Reminder" />
        <View style={styles.settingRow}>
          <Text style={styles.settingText}>Enable daily reminder</Text>
          <Switch value={settings.dailyEnabled} onValueChange={(dailyEnabled) => updateSettings({ dailyEnabled })} />
        </View>
        <View style={styles.timeRow}>
          <Text style={styles.timeText}>{formatTime(settings.hour, settings.minute)}</Text>
          <View style={styles.stepperGroup}>
            <StepperButton label="H-" onPress={() => updateSettings({ hour: (settings.hour + 23) % 24 })} />
            <StepperButton label="H+" onPress={() => updateSettings({ hour: (settings.hour + 1) % 24 })} />
            <StepperButton label="M-" onPress={() => updateSettings({ minute: (settings.minute + 55) % 60 })} />
            <StepperButton label="M+" onPress={() => updateSettings({ minute: (settings.minute + 5) % 60 })} />
          </View>
        </View>
      </View>

      <View style={styles.panel}>
        <SettingsTitle icon="chatbubble-ellipses-outline" title="Weekly Customer Reminders" />
        <Text style={styles.helperText}>Store the reminder preference for weekly customer follow-ups.</Text>
        <View style={styles.settingRow}>
          <Text style={styles.settingText}>Enable weekly SMS</Text>
          <Switch value={settings.weeklyEnabled} onValueChange={(weeklyEnabled) => updateSettings({ weeklyEnabled })} />
        </View>
      </View>

      <View style={styles.panel}>
        <SettingsTitle icon="shield-checkmark-outline" title="Security" />
        <Pressable style={styles.secondaryButton} onPress={() => setPinModalOpen(true)}>
          <Ionicons name="key-outline" size={18} color="#172554" />
          <Text style={styles.secondaryButtonText}>Change PIN</Text>
        </Pressable>
      </View>

      <Modal transparent visible={pinModalOpen} animationType="fade" onRequestClose={() => setPinModalOpen(false)}>
        <View style={styles.modalScrim}>
          <View style={styles.modalCard}>
            <Text style={styles.modalTitle}>Change PIN</Text>
            <TextInput
              value={newPin}
              onChangeText={(value) => setNewPin(value.replace(/\D/g, "").slice(0, 4))}
              placeholder="Enter new 4-digit PIN"
              placeholderTextColor="#94a3b8"
              secureTextEntry
              keyboardType="number-pad"
              maxLength={4}
              style={styles.input}
            />
            <View style={styles.modalActions}>
              <Pressable style={styles.cancelButton} onPress={() => setPinModalOpen(false)}>
                <Text style={styles.cancelButtonText}>Cancel</Text>
              </Pressable>
              <Pressable style={styles.modalSaveButton} onPress={changePin}>
                <Text style={styles.primaryButtonText}>Save</Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </ScreenFrame>
  );
}

function ScreenFrame({ title, onBack, children }) {
  return (
    <View style={styles.flex}>
      <View style={styles.subHeader}>
        <IconButton icon="arrow-back" onPress={onBack} />
        <Text style={styles.subHeaderTitle}>{title}</Text>
        <View style={styles.subHeaderSpacer} />
      </View>
      <ScrollView contentContainerStyle={styles.screenContent}>{children}</ScrollView>
    </View>
  );
}

function IconButton({ icon, onPress }) {
  return (
    <Pressable style={styles.iconButton} onPress={onPress}>
      <Ionicons name={icon} size={20} color="#ffffff" />
    </Pressable>
  );
}

function SummaryCard({ label, value, detail, color }) {
  return (
    <View style={styles.summaryCard}>
      <Text style={styles.summaryLabel}>{label}</Text>
      <Text style={[styles.summaryValue, { color }]} numberOfLines={1} adjustsFontSizeToFit>
        {value}
      </Text>
      <Text style={styles.summaryDetail}>{detail}</Text>
    </View>
  );
}

function TransactionItem({ item, onLongPress }) {
  const initial = item.customerName.trim().charAt(0).toUpperCase() || "N";
  return (
    <Pressable style={styles.transactionItem} onLongPress={onLongPress}>
      <View style={styles.transactionAvatar}>
        <Text style={styles.transactionAvatarText}>{initial}</Text>
      </View>
      <View style={styles.transactionBody}>
        <Text style={styles.transactionName}>{item.customerName}</Text>
        <Text style={styles.transactionMeta}>{item.phoneNumber || "No phone"}</Text>
        <Text style={styles.transactionMeta}>{item.timestamp}</Text>
      </View>
      <Text style={styles.transactionAmount}>Rs. {Number(item.amount).toFixed(2)}</Text>
    </Pressable>
  );
}

function LabeledInput({ label, ...inputProps }) {
  return (
    <View style={styles.fieldBlock}>
      <Text style={styles.fieldLabel}>{label}</Text>
      <TextInput placeholderTextColor="#94a3b8" style={[styles.input, inputProps.multiline && styles.textArea]} {...inputProps} />
    </View>
  );
}

function SettingsTitle({ icon, title }) {
  return (
    <View style={styles.panelTitleRow}>
      <Ionicons name={icon} size={19} color="#172554" />
      <Text style={styles.panelTitle}>{title}</Text>
    </View>
  );
}

function StepperButton({ label, onPress }) {
  return (
    <Pressable style={styles.stepperButton} onPress={onPress}>
      <Text style={styles.stepperText}>{label}</Text>
    </Pressable>
  );
}

function formatTime(hour, minute) {
  const amPm = hour < 12 ? "AM" : "PM";
  const displayHour = hour === 0 ? 12 : hour > 12 ? hour - 12 : hour;
  return `${String(displayHour).padStart(2, "0")}:${String(minute).padStart(2, "0")} ${amPm}`;
}

const styles = StyleSheet.create({
  appShell: {
    flex: 1,
    backgroundColor: "#f1f5f9"
  },
  flex: {
    flex: 1
  },
  loadingScreen: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#172554"
  },
  loadingTitle: {
    color: "#ffffff",
    fontSize: 20,
    fontWeight: "800"
  },
  loginScreen: {
    flex: 1,
    backgroundColor: "#172554"
  },
  loginContent: {
    flex: 1,
    justifyContent: "center",
    padding: 24
  },
  brandBadge: {
    width: 72,
    height: 72,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#2563eb",
    marginBottom: 18
  },
  loginTitle: {
    color: "#ffffff",
    fontSize: 30,
    fontWeight: "800"
  },
  loginSubtitle: {
    color: "#bfdbfe",
    fontSize: 15,
    marginTop: 6,
    marginBottom: 22
  },
  pinInput: {
    height: 54,
    borderRadius: 8,
    backgroundColor: "#ffffff",
    paddingHorizontal: 16,
    fontSize: 18,
    marginBottom: 14
  },
  primaryButton: {
    minHeight: 48,
    borderRadius: 8,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 8,
    backgroundColor: "#172554",
    marginTop: 12,
    paddingHorizontal: 16
  },
  primaryButtonText: {
    color: "#ffffff",
    fontWeight: "800",
    fontSize: 15
  },
  header: {
    backgroundColor: "#172554",
    paddingHorizontal: 16,
    paddingTop: 14,
    paddingBottom: 16,
    flexDirection: "row",
    alignItems: "center",
    gap: 12
  },
  headerTextBlock: {
    flex: 1,
    minWidth: 0
  },
  headerTitle: {
    color: "#ffffff",
    fontSize: 18,
    fontWeight: "800"
  },
  headerDate: {
    color: "#bfdbfe",
    fontSize: 12,
    marginTop: 2
  },
  headerActions: {
    flexDirection: "row",
    gap: 8
  },
  iconButton: {
    width: 36,
    height: 36,
    borderRadius: 8,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(255,255,255,0.14)"
  },
  dashboardContent: {
    padding: 12,
    paddingBottom: 24
  },
  summaryRow: {
    flexDirection: "row",
    gap: 12,
    marginBottom: 12
  },
  summaryCard: {
    flex: 1,
    minHeight: 118,
    borderRadius: 8,
    backgroundColor: "#ffffff",
    padding: 14,
    justifyContent: "center",
    borderWidth: 1,
    borderColor: "#e2e8f0"
  },
  summaryLabel: {
    color: "#64748b",
    fontSize: 10,
    fontWeight: "800"
  },
  summaryValue: {
    fontSize: 25,
    fontWeight: "900",
    marginVertical: 3
  },
  summaryDetail: {
    color: "#94a3b8",
    fontSize: 11
  },
  panel: {
    borderRadius: 8,
    backgroundColor: "#ffffff",
    padding: 14,
    marginBottom: 12,
    borderWidth: 1,
    borderColor: "#e2e8f0"
  },
  panelTitleRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 7,
    marginBottom: 12
  },
  panelTitle: {
    color: "#172554",
    fontSize: 15,
    fontWeight: "800"
  },
  formRow: {
    flexDirection: "row",
    gap: 10
  },
  formHalfInput: {
    flex: 1
  },
  input: {
    minHeight: 46,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: "#cbd5e1",
    backgroundColor: "#f8fafc",
    color: "#0f172a",
    paddingHorizontal: 12,
    fontSize: 14,
    marginBottom: 10
  },
  sectionHeader: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingHorizontal: 4,
    paddingTop: 4,
    paddingBottom: 8
  },
  sectionTitle: {
    color: "#0f172a",
    fontSize: 15,
    fontWeight: "800"
  },
  clearText: {
    color: "#dc2626",
    fontSize: 13,
    fontWeight: "800"
  },
  emptyText: {
    backgroundColor: "#ffffff",
    borderRadius: 8,
    padding: 20,
    textAlign: "center",
    color: "#64748b",
    borderWidth: 1,
    borderColor: "#e2e8f0"
  },
  transactionItem: {
    minHeight: 82,
    flexDirection: "row",
    alignItems: "center",
    backgroundColor: "#ffffff",
    padding: 12,
    borderBottomWidth: 1,
    borderBottomColor: "#e2e8f0"
  },
  transactionAvatar: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: "#dbeafe",
    alignItems: "center",
    justifyContent: "center",
    marginRight: 12
  },
  transactionAvatarText: {
    color: "#172554",
    fontWeight: "900",
    fontSize: 16
  },
  transactionBody: {
    flex: 1,
    minWidth: 0
  },
  transactionName: {
    color: "#0f172a",
    fontWeight: "800",
    fontSize: 14
  },
  transactionMeta: {
    color: "#64748b",
    fontSize: 12,
    marginTop: 1
  },
  transactionAmount: {
    color: "#15803d",
    fontSize: 14,
    fontWeight: "900",
    marginLeft: 10
  },
  subHeader: {
    backgroundColor: "#172554",
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 14,
    flexDirection: "row",
    alignItems: "center"
  },
  subHeaderTitle: {
    flex: 1,
    color: "#ffffff",
    fontSize: 18,
    fontWeight: "800",
    textAlign: "center"
  },
  subHeaderSpacer: {
    width: 36
  },
  screenContent: {
    padding: 16,
    paddingBottom: 28
  },
  profileHero: {
    alignItems: "center",
    justifyContent: "center",
    minHeight: 150,
    borderRadius: 8,
    backgroundColor: "#ffffff",
    borderWidth: 1,
    borderColor: "#e2e8f0",
    marginBottom: 14
  },
  profileAvatar: {
    width: 80,
    height: 80,
    borderRadius: 40,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#dbeafe"
  },
  profileHeroTitle: {
    color: "#172554",
    fontSize: 16,
    fontWeight: "800",
    marginTop: 10
  },
  fieldBlock: {
    marginBottom: 4
  },
  fieldLabel: {
    color: "#64748b",
    fontSize: 12,
    marginBottom: 5,
    fontWeight: "700"
  },
  textArea: {
    minHeight: 82,
    paddingTop: 12,
    textAlignVertical: "top"
  },
  settingRow: {
    minHeight: 44,
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between"
  },
  settingText: {
    color: "#0f172a",
    fontSize: 14,
    fontWeight: "600",
    flex: 1,
    paddingRight: 12
  },
  timeRow: {
    borderRadius: 8,
    backgroundColor: "#f8fafc",
    borderWidth: 1,
    borderColor: "#e2e8f0",
    padding: 10,
    marginTop: 10
  },
  timeText: {
    color: "#172554",
    fontWeight: "900",
    fontSize: 18,
    marginBottom: 10
  },
  stepperGroup: {
    flexDirection: "row",
    flexWrap: "wrap",
    gap: 8
  },
  stepperButton: {
    minWidth: 56,
    height: 34,
    borderRadius: 8,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#dbeafe"
  },
  stepperText: {
    color: "#172554",
    fontWeight: "900"
  },
  helperText: {
    color: "#64748b",
    fontSize: 12,
    lineHeight: 17,
    marginBottom: 8
  },
  secondaryButton: {
    minHeight: 46,
    borderRadius: 8,
    alignItems: "center",
    justifyContent: "center",
    flexDirection: "row",
    gap: 8,
    backgroundColor: "#dbeafe"
  },
  secondaryButtonText: {
    color: "#172554",
    fontWeight: "800"
  },
  modalScrim: {
    flex: 1,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "rgba(15,23,42,0.55)",
    padding: 24
  },
  modalCard: {
    width: "100%",
    maxWidth: 360,
    borderRadius: 8,
    backgroundColor: "#ffffff",
    padding: 16
  },
  modalTitle: {
    color: "#0f172a",
    fontSize: 18,
    fontWeight: "800",
    marginBottom: 12
  },
  modalActions: {
    flexDirection: "row",
    justifyContent: "flex-end",
    gap: 10
  },
  cancelButton: {
    minHeight: 42,
    paddingHorizontal: 16,
    borderRadius: 8,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#f1f5f9"
  },
  cancelButtonText: {
    color: "#334155",
    fontWeight: "800"
  },
  modalSaveButton: {
    minHeight: 42,
    paddingHorizontal: 18,
    borderRadius: 8,
    alignItems: "center",
    justifyContent: "center",
    backgroundColor: "#172554"
  }
});
