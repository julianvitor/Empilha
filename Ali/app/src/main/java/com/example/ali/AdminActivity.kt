package com.example.ali

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AdminActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        databaseHelper = DatabaseHelper(this)

        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val adapter = UsoAdapter(databaseHelper.getUsuariosERetiradasDevolucao())
        recyclerView.adapter = adapter

        //botão voltar
        val buttonBack: MaterialButton = findViewById(R.id.buttonBack)
        buttonBack.setOnClickListener{
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private inner class UsoAdapter(private val listaDeUsuarios: List<Quadra<String, String, String, String>>) :
        RecyclerView.Adapter<UsoAdapter.UsoViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UsoViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_uso_layout, parent, false)
            return UsoViewHolder(view)
        }

        override fun onBindViewHolder(holder: UsoViewHolder, position: Int) {
            val (nome, retirada, devolucao, _) = listaDeUsuarios[position]

            holder.nomeTextView.text = nome
            holder.retiradaTextView.text = retirada
            holder.devolucaoTextView.text = devolucao

            val diferencaTempo = calcularDiferencaTempo(retirada, devolucao)
            holder.tempoTextView.text = formatarTempo(diferencaTempo)
        }

        override fun getItemCount(): Int {
            return listaDeUsuarios.size
        }

        inner class UsoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val nomeTextView: TextView = itemView.findViewById(R.id.nome_text_view)
            val retiradaTextView: TextView = itemView.findViewById(R.id.retirada_text_view)
            val devolucaoTextView: TextView = itemView.findViewById(R.id.devolucao_text_view)
            val tempoTextView: TextView = itemView.findViewById(R.id.tempo_text_view)
        }
    }

    private fun calcularDiferencaTempo(retirada: String, devolucao: String): Long {
        if (retirada.isEmpty() || devolucao.isEmpty()) {
            return 0
        }

        val formato = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dataRetirada = formato.parse(retirada)
        val dataDevolucao = formato.parse(devolucao)

        return dataDevolucao.time - dataRetirada.time
    }

    private fun formatarTempo(tempo: Long): String {
        val horas = TimeUnit.MILLISECONDS.toHours(tempo)
        val minutos = TimeUnit.MILLISECONDS.toMinutes(tempo) % 60
        val segundos = TimeUnit.MILLISECONDS.toSeconds(tempo) % 60
        return String.format("%02d:%02d:%02d", horas, minutos, segundos)
    }
}
