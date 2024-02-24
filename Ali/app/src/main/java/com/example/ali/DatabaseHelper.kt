package com.example.ali
import android.content.ContentValues
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.charset.Charset
import java.util.*

data class Quadra<T, U, V, W>(val first: T, val second: U, val third: V, val fourth: W)

class DatabaseHelper(context: Context) {
    internal val usuariosFileName = "usuarios.json"
    private val usosFileName = "usos.json"
    private val context: Context = context.applicationContext

    fun registrarDevolucao(uid: String) {
        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        var devolucaoRegistrada = false

        // Iterar sobre todos os registros para encontrar aqueles com o UID correspondente
        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            if (uso.optString("uid") == uid) {
                uso.put("devolucao", getDataHoraAtual())
                devolucaoRegistrada = true
            }
        }

        // Verificar se a devolução foi registrada
        if (!devolucaoRegistrada) {
            // Lidar com o caso em que nenhum registro com o UID correspondente foi encontrado
            // Aqui você pode lançar uma exceção, enviar uma mensagem de erro ou tomar outra ação adequada ao seu aplicativo
            return
        }

        // Salvar as alterações no arquivo JSON
        writeJsonToFile(usosJson.toString(), usosFileName)
    }

    fun registrarUso(apelido: String, uid: String, doca: String) {
        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        // Verificar se já existe um registro para o usuário
        var found = false
        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            if (uso.optString("apelido") == apelido) {
                uso.put("retirada", getDataHoraAtual())
                uso.put("uid", uid)
                uso.put("doca", doca)
                found = true
                break
            }
        }

        if (!found) {
            val novoUso = JSONObject()
            novoUso.put("apelido", apelido)
            novoUso.put("retirada", getDataHoraAtual())
            novoUso.put("uid", uid)
            novoUso.put("doca", doca)
            usosArray.put(novoUso)
        }

        usosJson.put("usos", usosArray)
        writeJsonToFile(usosJson.toString(), usosFileName)
    }

    fun verificarCredenciais(apelido: String, senha: String): Boolean {
        val usuariosJson = loadJsonFromFile(usuariosFileName) ?: JSONObject()
        val usuariosArray = usuariosJson.optJSONArray("usuarios") ?: JSONArray()

        // Verificar se há um usuário com as credenciais fornecidas
        for (i in 0 until usuariosArray.length()) {
            val usuario = usuariosArray.optJSONObject(i)
            if (usuario.optString("apelido") == apelido && usuario.optString("senha") == senha) {
                return true
            }
        }
        return false
    }

    fun getUsuariosERetiradas(): List<Pair<String, String>> {
        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        val usuariosERetiradas = mutableListOf<Pair<String, String>>()

        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            val apelido = uso.optString("apelido")
            val retirada = uso.optString("retirada")
            usuariosERetiradas.add(Pair(apelido, retirada))
        }

        return usuariosERetiradas
    }

    fun getUsuariosERetiradasDevolucao(): List<Quadra<String, String, String, String>> {
        val usosJson = loadJsonFromFile(usosFileName) ?: JSONObject()
        val usosArray = usosJson.optJSONArray("usos") ?: JSONArray()

        val usuariosERetiradasDevolucao = mutableListOf<Quadra<String, String, String, String>>()

        for (i in 0 until usosArray.length()) {
            val uso = usosArray.optJSONObject(i)
            val apelido = uso.optString("apelido")
            val retirada = uso.optString("retirada")
            val devolucao = uso.optString("devolucao")
            val uid = uso.optString("uid")
            usuariosERetiradasDevolucao.add(Quadra(apelido, retirada, devolucao, uid))
        }

        return usuariosERetiradasDevolucao
    }

    private fun loadJsonFromAsset(fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charset.defaultCharset())
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun writeJsonToFile(jsonData: String, fileName: String) {
        try {
            val fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
            fileOutputStream.write(jsonData.toByteArray())
            fileOutputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadJsonFromFile(fileName: String): JSONObject? {
        val file = File(context.filesDir, fileName)
        return if (file.exists()) {
            try {
                val bufferedReader = file.bufferedReader()
                val jsonString = bufferedReader.use { it.readText() }
                JSONObject(jsonString)
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    private fun getDataHoraAtual(): String {
        val calendar = Calendar.getInstance()
        return "${calendar.get(Calendar.YEAR)}-${calendar.get(Calendar.MONTH) + 1}-${calendar.get(Calendar.DAY_OF_MONTH)} " +
                "${calendar.get(Calendar.HOUR_OF_DAY)}:${calendar.get(Calendar.MINUTE)}:${calendar.get(Calendar.SECOND)}"
    }
}
