package com.standing.`in`.line

import android.content.ContentValues.TAG
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.standing.`in`.line.ui.theme.StandingInLineTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.composable

val db = Firebase.firestore

class MainViewModel : ViewModel() {
    var isAuth by mutableStateOf(false)
        private set

    fun onRegisterSuccess() {
        isAuth = true
    }
}

data class NavItem(val title: String, val icon: ImageVector, val name: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        var vm = MainViewModel()
        var deviceID = Settings.Secure.getString(this.contentResolver, Settings.Secure.ANDROID_ID)
        println("DEVICE ID IS: $deviceID")
        val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("deviceId", deviceID)
        editor.apply()

        val docRef = db.collection("users").document(deviceID)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document.data != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    val sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("userName", document.data?.get("user_name").toString())
                    editor.apply()
                    vm.onRegisterSuccess()
                } else {
                    Log.d(TAG, "No such document")
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "get failed with ", exception)
            }

        super.onCreate(savedInstanceState)
        setContent {
            StandingInLineTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App(vm)
                }
            }
        }
    }
}

@Composable
fun App(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = if (viewModel.isAuth) "main" else "login") {

        composable("main") {
            MainComponent(navController)
        }
        composable("login") {
            Login(onRegisterSuccess = {
                viewModel.onRegisterSuccess()
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("detail/{itemId}") { backStackEntry ->
            val itemId = backStackEntry.arguments?.getString("itemId")
            DetailScreen(itemId = itemId, navController = navController)
        }
    }
}
class PreferencesManager(context: Context) {
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    fun saveData(key: String, value: String) {
        val editor = sharedPreferences.edit()
        editor.putString(key, value)
        editor.apply()
    }

    fun getData(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }
}


@Composable
fun Login(onRegisterSuccess: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LoginForm(onRegisterSuccess = onRegisterSuccess, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun LoginForm(modifier: Modifier = Modifier, onRegisterSuccess: () -> Unit){
    val context = LocalContext.current

    var nameInput by remember {
        mutableStateOf("")
    }
    var isNameValid  by remember {
        mutableStateOf(false)
    }


    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val deviceId = sharedPreferences.getString("deviceId", "")


    Box(modifier = modifier.padding(all = 12.dp), contentAlignment = Alignment.Center){
        Column(modifier = modifier) {
            Text(text = "Введите ваше имя", fontWeight = FontWeight.Bold, fontSize = 32.sp, modifier = Modifier.padding(bottom = 24.dp))
            TextField(label = { Text(text = "Имя")},value = nameInput, onValueChange = { input ->
                isNameValid =  isValidText(input)
                nameInput = input
            },  isError = nameInput.isNotEmpty() && !isValidText(nameInput),  modifier = Modifier.fillMaxWidth(), shape  = RoundedCornerShape(4.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done))
            if (!isNameValid && nameInput.isNotEmpty()){
                Row (modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp), verticalAlignment = Alignment.Top){
                    Icon(painter = painterResource(id = R.drawable.error), contentDescription = "Error", tint = Color.Red, modifier = Modifier.padding(end = 4.dp))
                    Text(text = "В имени не должно быть символов или пробелов!", modifier = Modifier, color = Color.Red,fontSize = 16.sp)

                }
            }
             }
        Button(onClick = {
            if (isNameValid) {
                generateUniqueInviteCode { inviteCode ->
                    val sharedPreferences = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putString("userName", nameInput)
                    editor.putString("inviteCode", inviteCode)
                    editor.apply()
                    registerUser(
                        userName = nameInput,
                        deviceId = deviceId,
                        inviteCode = inviteCode,
                        onRegisterSuccess = onRegisterSuccess
                    )
                }
            }
        }, shape = RoundedCornerShape(4.dp), modifier = modifier.then(
            if (isNameValid) Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(72.dp)
            else Modifier
                .alpha(0.5F)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(72.dp)
        )
        ) {
            Text(text = "Начать", fontSize = 24.sp)
        }


    }
}

fun generateUniqueInviteCode(callback: (String) -> Unit) {
    val inviteCode = generateInviteCode()
    val docRef = db.collection("codes").document(inviteCode)
    docRef.get().addOnSuccessListener { document ->
        if (document.data != null) {
            Log.d(TAG, "Document exists, generating a new code")
            generateUniqueInviteCode(callback) // Рекурсия, если код уже существует
        } else {
            Log.d(TAG, "No such document, using invite code: $inviteCode")
            callback(inviteCode) // Возвращаем уникальный код через callback
        }
    }.addOnFailureListener { exception ->
        Log.d(TAG, "get failed with ", exception)
        callback(inviteCode) // В случае ошибки продолжаем с текущим кодом
    }
}

fun registerUser(userName: String,inviteCode: String, deviceId: String?, onRegisterSuccess: () -> Unit) {
    val user = hashMapOf(
        "user_name" to userName,
        "device_id" to deviceId,
        "created_at" to FieldValue.serverTimestamp(),
        "invite_code" to inviteCode
    )



    if (deviceId != null) {
        val userRef = db.collection("users").document(deviceId)

        userRef.set(user)
            .addOnSuccessListener {
                Log.d(TAG, "New user successfully created!")

                val queue = hashMapOf(
                    "admin" to userRef,
                    "invite_code" to inviteCode,
                )
                db.collection("queues")
                    .add(queue)
                    .addOnSuccessListener {
                        Log.d(TAG, "New queue successfully created!")
                        onRegisterSuccess()
                    }
                    .addOnFailureListener { e -> Log.w(TAG, "Error writing document", e) }
            }
            .addOnFailureListener { e -> Log.w(TAG, "Error writing user document", e) }
    }

}
//fun loginUser(){
//
//}

fun isValidText(text: String): Boolean {
    return text.matches(Regex("[a-zA-Z0-9А-Яа-я]+"))
}

@Composable
fun DetailScreen(itemId: String?, navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Детальный экран: $itemId", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.popBackStack() }) {
            Text(text = "Назад")
        }
    }
}

@Composable
fun QueueScreen(onItemClick: (String) -> Unit) {
//    var selectedTabIndex by remember {
//        mutableStateOf(0)
//    }
//    Column(modifier = Modifier.fillMaxSize()) {
//
//        TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier
//            .height(72.dp) ) {
//            Tab(selected = selectedTabIndex == 0, onClick = {
//                selectedTabIndex = 0
//            }) {
//                Text(text = "Все", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
//            }
//            Tab(selected = selectedTabIndex == 1, onClick = {
//                selectedTabIndex = 1
//            }) {
//                Text(text = "Мои", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
//            }
//        }
//        when (selectedTabIndex) {
//            0 -> allQueues()
//            1 -> myQueues()
//        }
//
////        Button(onClick = { onItemClick("123") }) {
////            Text(text = "Перейти к детали")
////        }
//    }

    allQueues()
}

@Composable
fun myQueues(){
    Box(modifier = Modifier.fillMaxSize()){
        FloatingActionButton(onClick = { /*TODO*/ }, modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 16.dp)
            .padding(end = 24.dp)) {
            Icon(Icons.Filled.Add, "Floating action button.")
        }
    }
}
@Composable
fun allQueues(){
    var isDialogOpen by remember {
        mutableStateOf(false)
    }

    Box(modifier = Modifier.fillMaxSize()){
        FloatingActionButton(onClick = { isDialogOpen = true }, modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 16.dp)
            .padding(end = 24.dp)) {
            Icon(Icons.Filled.Add, "Floating action button.")
        }
        if (isDialogOpen){
            queueDialog(onClose = {isDialogOpen = false})
        }

    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun queueDialog(onClose : () -> Unit){
    var textFieldInput by remember {
        mutableStateOf("")
    }

    Box(contentAlignment = Alignment.Center, modifier = Modifier
        .fillMaxSize()
        .alpha(0.5f)
        .background(color = Color.Gray)){
        AlertDialog(onDismissRequest = { onClose() }, confirmButton = { Button(onClick = { }, enabled = textFieldInput != "") {
            Text(text = "Войти")
        } } , text = {
            TextField(value = textFieldInput, onValueChange = {textFieldInput = it}, colors = TextFieldDefaults.textFieldColors(containerColor = Color.White), label = { Text(
                text = "Код приглашения"
            )})
        }, title = {Text(text = "Встать в очередь")}, dismissButton = {
            Button(onClick = {onClose() }) {
                Text(text = "Отмена")
            }
        })
    }


}

@Composable
fun SettingsScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "Настройки", fontSize = 24.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InviteScreen() {

    val clipboardManager: ClipboardManager = LocalClipboardManager.current
    var inviteCode by remember { mutableStateOf("")}

    val context = LocalContext.current
    var pm = PreferencesManager(context = context)
    inviteCode = pm.getData("inviteCode", "unknown")
    var adminName = pm.getData("userName", "unknown")

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Код приглашения:", fontSize = 24.sp)
                Button(onClick = {
                    clipboardManager.setText(AnnotatedString((inviteCode)))
                }, colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), contentPadding = PaddingValues(), modifier = Modifier){
                    Text(text = "$inviteCode",textDecoration = TextDecoration.Underline, color = Color.Blue, modifier = Modifier
                        .width(IntrinsicSize.Min)
                        .padding(start = 8.dp),  fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Text(text = "Администратор - $adminName")
                Row {

                }
            }
        }
    }
}

@Composable
fun MainComponent(navController: NavHostController) {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        NavItem("Очереди", Icons.Filled.Face, "queue"),
        NavItem("Администратор", Icons.Filled.AccountBox, "invite"),
        NavItem("Настройки", Icons.Filled.Settings, "settings"),
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            when (selectedItem) {
                0 -> QueueScreen { itemId ->
                    navController.navigate("detail/$itemId")
                }
                1 -> InviteScreen()
                2 -> SettingsScreen()
            }
        }

        NavigationBar(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title, fontSize = 14.sp) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }
    }
}



@Composable
fun NavBar() {
    var selectedItem by remember { mutableIntStateOf(0) }
    val items = listOf(
        NavItem("Очередь", Icons.Filled.Face, "queue"),
        NavItem("Инвайт", Icons.Filled.Add, "invite"),
        NavItem("Настройки", Icons.Filled.Settings, "settings"),
    )
    println("New route $selectedItem")

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(1f)) // Spacer to push the NavigationBar to the bottom

        NavigationBar(modifier = Modifier.fillMaxWidth()) {
            items.forEachIndexed { index, item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.title) },
                    label = { Text(item.title, fontSize = 18.sp) },
                    selected = selectedItem == index,
                    onClick = { selectedItem = index }
                )
            }
        }
    }
}

@Preview
@Composable
fun AppPreview(){
    App()
}

fun generateInviteCode(): String {
    val allowedChars = ('A'..'Z') + ('0'..'9')
    return (1..5)
        .map { allowedChars.random() }
        .joinToString("")
}