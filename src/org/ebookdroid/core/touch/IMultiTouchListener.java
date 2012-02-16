package org.ebookdroid.core.touch;


public interface IMultiTouchListener {

    void onTwoFingerPinchEnd();

    void onTwoFingerPinch(float oldDistance, float newDistance);

    void onTwoFingerTap();

}
