package io.ristretto.decaptcha;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import io.ristretto.decaptcha.captcha.ParcableCaptchaResult;

/**
 * A simple {@link Fragment} subclass.
 */
public class ResultFragment extends Fragment {

    private static final String ARGS_RESULT = "result";

    public ResultFragment() {
        super();
        // Required empty public constructor
    }


    public static ResultFragment newInstance(ParcableCaptchaResult captchaResult) {
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGS_RESULT, captchaResult);
        ResultFragment fragment = new ResultFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    public void updateResult(ParcableCaptchaResult captchaResult) {
        // Does nothing yet
    }
}
