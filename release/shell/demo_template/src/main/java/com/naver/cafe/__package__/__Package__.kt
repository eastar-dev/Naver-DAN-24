package dev.eastar.naverdan24.__package__

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import java.lang.reflect.Method

@AndroidEntryPoint
class __Package__ : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val easterEggClz = __Package__EasterEgg::class.java
        val easterEggs = EasterEggRunner.getMethods(easterEggClz)
        EasterEggRunner.autoRunEasterEgg(easterEggClz, this)
        setContent {
            AppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MainScreen(easterEggs) {
                        EasterEggRunner.invokeMethod(easterEggClz, it, this)
                    }
                }
            }
        }
    }
}

@Composable
private fun MainScreen(easterEggs: List<Method>, itemClicked: (Method) -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        val navController = rememberNavController()
        val interceptItemClicked = { item: Method ->
            when {
                isNav(item) -> navController.navigate(nav(item))
                else -> itemClicked(item)
            }
        }
        NavHost(navController = navController, startDestination = "main") {
            composable("main") { MethodList(easterEggs, interceptItemClicked) }
            composable("detail") { DetailScreen() }
        }
    }
}

private fun nav(item: Method) = item.name.replace("_", "").drop("navigate".length)
private fun isNav(item: Method) = item.name.replace("_", "").startsWith("navigate")

@Composable
fun DetailScreen() {
    Text(text = "여기를 변경해서 Composable 확인해 보세요")
}

@Composable
fun MethodList(list: List<Method>, itemClicked: (Method) -> Unit) {
    LazyColumn(
        Modifier
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
    ) {
        itemsIndexed(
            list,
            key = { _, item -> item.name },
            contentType = { index, _ -> index % 2 }
        ) { index, item ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 50.dp)
                    .clickable { itemClicked(item) }
                    .background(if (index % 2 == 0) Color.Gray else MaterialTheme.colorScheme.surface)
                    .padding(4.dp),
                verticalArrangement = Arrangement.Center

            ) {
                Text(item.name, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@AppPreview
@Composable
fun __Package__Preview() {
    AppTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MethodList(EasterEggRunner.getMethods(__Package__EasterEgg::class.java)) {}
        }
    }
}
