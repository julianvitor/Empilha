package com.example.ali
import android.os.Bundle
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AdminActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        databaseHelper = DatabaseHelper(this)

        val tableLayout: TableLayout = findViewById(R.id.table_layout)

        val usuariosERetiradasDevolucao = databaseHelper.getUsuariosERetiradasDevolucao()

        for (usuarioERetiradaDevolucao in usuariosERetiradasDevolucao) {
            val nome = usuarioERetiradaDevolucao.first
            val retirada = usuarioERetiradaDevolucao.second
            val devolucao = usuarioERetiradaDevolucao.third
            val uid = usuarioERetiradaDevolucao.fourth

            val row = TableRow(this)

            addTextViewToRow(row, nome)
            addTextViewToRow(row, retirada)
            addTextViewToRow(row, devolucao)
            addTextViewToRow(row, uid)

            // Calcular a diferença de tempo entre retirada e devolução para este usuário
            val diferencaTempo = calcularDiferencaTempo(retirada, devolucao)

            // Adicionar a diferença de tempo à linha da tabela para este usuário
            addTextViewToRow(row, formatarTempo(diferencaTempo))

            tableLayout.addView(row)
        }
    }

    private fun addTextViewToRow(row: TableRow, text: String) {
        val textView = TextView(this)
        textView.text = text
        textView.setPadding(8, 8, 8, 8)
        row.addView(textView)
    }

    // Função para calcular a diferença de tempo em milissegundos
    private fun calcularDiferencaTempo(retirada: String, devolucao: String): Long {
        // Verificar se os tempos de retirada e devolução não estão vazios
        if (retirada.isEmpty() || devolucao.isEmpty()) {
            // Se um dos tempos estiver vazio, retornar 0 ou outro valor adequado, dependendo da sua lógica
            return 0
        }

        // Se os tempos não estiverem vazios, calcular a diferença de tempo normalmente
        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dataRetirada = formato.parse(retirada)
        val dataDevolucao = formato.parse(devolucao)
        return dataDevolucao.time - dataRetirada.time
    }


    // Função para formatar o tempo em horas, minutos e segundos
    private fun formatarTempo(tempo: Long): String {
        val horas = TimeUnit.MILLISECONDS.toHours(tempo)
        val minutos = TimeUnit.MILLISECONDS.toMinutes(tempo) % 60
        val segundos = TimeUnit.MILLISECONDS.toSeconds(tempo) % 60
        return String.format("%02d:%02d:%02d", horas, minutos, segundos)
    }
}