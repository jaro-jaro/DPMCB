package cz.jaro.dpmcb

import androidx.navigation.NavOptionsBuilder
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.ResultBackNavigator
import cz.jaro.dpmcb.ui.vybirator.Vysledek

object FakeNavigator : DestinationsNavigator, ResultBackNavigator<Vysledek> {
    override fun clearBackStack(route: String) = false
    override fun navigate(route: String, onlyIfResumed: Boolean, builder: NavOptionsBuilder.() -> Unit) = Unit
    override fun navigateUp() = false
    override fun popBackStack() = false
    override fun popBackStack(route: String, inclusive: Boolean, saveState: Boolean) = false
    override fun navigateBack() = Unit
    override fun setResult(result: Vysledek) = Unit
    override fun navigateBack(result: Vysledek) = Unit
}
