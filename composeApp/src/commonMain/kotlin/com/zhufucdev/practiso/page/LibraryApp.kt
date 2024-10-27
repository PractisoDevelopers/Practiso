package com.zhufucdev.practiso.page

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.zhufucdev.practiso.composable.FloatingPopupButton
import com.zhufucdev.practiso.composition.composeFromBottomUp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import practiso.composeapp.generated.resources.Res
import practiso.composeapp.generated.resources.baseline_import
import practiso.composeapp.generated.resources.create_para
import practiso.composeapp.generated.resources.import_para

@Composable
fun LibraryApp() {
    var showActions by remember {
        mutableStateOf(false)
    }

    composeFromBottomUp("fab") {
        FloatingPopupButton(
            expanded = showActions,
            onExpandedChange = { showActions = it }
        ) {
            item(
                label = { Text(stringResource(Res.string.import_para)) },
                icon = { Icon(painterResource(Res.drawable.baseline_import), contentDescription = null) },
                onClick = {  }
            )
            item(
                label = { Text(stringResource(Res.string.create_para)) },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {  }
            )
        }
    }

}
