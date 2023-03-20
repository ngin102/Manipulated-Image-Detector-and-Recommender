package com.example.manipulatedimagerecommenderanddetector.ui.main;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

/**
 * This is code provided by Android Studio.
 */
public class PageViewModel extends ViewModel {

    private MutableLiveData<Integer> mIndex = new MutableLiveData<>();

    public int getIndex()
    {
        return mIndex.getValue();
    }
    public void setIndex(int index) {
        mIndex.setValue(index);
    }
}