package com.example.ali

import android.provider.BaseColumns

object DatabaseContract {
    // Definir a estrutura da tabela
    object RegistroEntry : BaseColumns {
        const val _ID = "_id"
        const val TABLE_NAME = "registro"
        const val COLUMN_NOME = "nome"
        const val COLUMN_USUARIO = "usuario"
        const val COLUMN_SENHA = "senha"
        const val COLUMN_USOS = "usos"
        const val COLUMN_DEVOLUCAO = "devolucao"
    }
}