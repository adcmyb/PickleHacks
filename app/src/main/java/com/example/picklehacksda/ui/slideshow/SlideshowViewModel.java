package com.example.picklehacksda.ui.slideshow;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SlideshowViewModel extends ViewModel {

    private MutableLiveData<String> mText;

    public SlideshowViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("About Us: AdMe is a game developed with the desire to create joy out of something mundane.");
    }

    public LiveData<String> getText() {
        return mText;
    }
}