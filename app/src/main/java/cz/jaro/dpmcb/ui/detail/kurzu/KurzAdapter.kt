package cz.jaro.dpmcb.ui.detail.kurzu

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import cz.jaro.dpmcb.data.entities.Spoj
import cz.jaro.dpmcb.data.helperclasses.Cas
import cz.jaro.dpmcb.data.helperclasses.Smer.NEGATIVNI
import cz.jaro.dpmcb.data.helperclasses.Smer.POZITIVNI
import cz.jaro.dpmcb.data.helperclasses.UtilFunctions.zastavkySpoje
import cz.jaro.dpmcb.databinding.SpojBinding
import cz.jaro.dpmcb.ui.destinations.OdjezdyScreenDestination
import kotlinx.coroutines.runBlocking


class KurzAdapter(navigator: DestinationsNavigator, private val ctx: Context, seznam: List<Spoj>) :
    RecyclerView.Adapter<KurzAdapter.ViewHolder>() {

    class ViewHolder(binding: SpojBinding) : RecyclerView.ViewHolder(binding.root) {
        val llCasy = binding.llCasy
        val llZastavky = binding.llZastavky
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        val binding = SpojBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    private val spoje = seznam.map { spoj ->
        when (spoj.smer) {
            POZITIVNI -> runBlocking { spoj.zastavkySpoje() }
            NEGATIVNI -> runBlocking { spoj.zastavkySpoje().reversed() }
        }
    }.sortedBy { it.first().cas.toInt() }

    private val odjezdy = { z: String, cas: String ->
        navigator.navigate(
            OdjezdyScreenDestination(
                cas = cas,
                zastavka = z
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, pos: Int) {

        val spoj = spoje[holder.adapterPosition]

        holder.llZastavky.removeAllViews()
        holder.llCasy.removeAllViews()

        spoj.filter { it.cas != Cas.nikdy }.forEach {
            val z = it.nazevZastavky
            val cas = it.cas

            holder.llZastavky.addView(TextView(ctx).apply {
                text = z
                setOnClickListener { odjezdy(z, cas.toString()) }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                updateLayoutParams<LinearLayout.LayoutParams> {
                    minHeight = 48
                    minWidth = 48
                }
            })

            holder.llCasy.addView(TextView(ctx).apply {
                text = cas.toString()
                setOnClickListener { odjezdy(z, cas.toString()) }
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                updateLayoutParams<LinearLayout.LayoutParams> {
                    minHeight = 48
                    minWidth = 48
                }
            })
        }
    }

    override fun getItemCount(): Int = spoje.size
}
