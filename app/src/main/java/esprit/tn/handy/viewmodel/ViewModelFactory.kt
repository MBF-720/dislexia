package esprit.tn.handy.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LearnViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LearnViewModel(application) as T
        }
        if (modelClass.isAssignableFrom(TestViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TestViewModel(application) as T
        }
        if (modelClass.isAssignableFrom(TrainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TrainViewModel(application) as T
        }
        if (modelClass.isAssignableFrom(RanQuizViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RanQuizViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

