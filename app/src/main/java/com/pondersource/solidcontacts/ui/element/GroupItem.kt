package com.pondersource.solidcontacts.ui.element

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pondersource.shared.data.datamodule.contact.Group
import com.pondersource.solidcontacts.R

@Composable
fun GroupItem(
    group: Group,
    addable: Boolean = false,
    added: Boolean = false,
    onAddRemoveClicked: (Group) -> Unit = {},
    onClick: (Group) -> Unit = {}
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable{onClick(group)},
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                group.name,
                modifier = Modifier
                    .padding(12.dp)
            )

            if(addable) {
                if (added) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = null,
                        modifier = Modifier
                            .clickable { onAddRemoveClicked(group) }
                            .padding(12.dp),
                        tint = Color.Red
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier
                            .clickable { onAddRemoveClicked(group) }
                            .padding(12.dp),
                        tint = Color.Green
                    )
                }
            }
        }

    }
}