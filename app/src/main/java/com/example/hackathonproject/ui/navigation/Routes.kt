package com.example.hackathonproject.ui.navigation

object Routes {
    const val SPLITTER = "splitter"
    const val HISTORY = "history"
    const val PEOPLE = "people"
    const val SCAN = "scan"
    const val PERSON_EDIT = "person_edit/{personId}"
    const val BILL_DETAIL = "bill_detail/{billId}"

    /** [personId] = 0 means "create a new person". */
    fun personEdit(personId: Long): String = "person_edit/$personId"
    fun billDetail(billId: Long): String = "bill_detail/$billId"
}
