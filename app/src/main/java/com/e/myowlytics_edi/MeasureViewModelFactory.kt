package com.e.myowlytics_edi

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MeasureViewModelFactory(private val application: Application) : ViewModelProvider.Factory{

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if(modelClass.isAssignableFrom(MeasureViewModel::class.java)){
            return MeasureViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}