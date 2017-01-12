package io.ristretto.decaptcha;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link StartFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link StartFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StartFragment extends Fragment {
    private static final String ARG_DEFAULT_URL = "default_url";

    private String defaultUrl;
    private boolean textWasChanged = false;

    private OnFragmentInteractionListener mListener;

    public StartFragment() {
        // Required empty public constructor
    }

    public static StartFragment newInstance(String url, String errorMessage) {
        StartFragment fragment = new StartFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEFAULT_URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            defaultUrl = savedInstanceState.getString(ARG_DEFAULT_URL);
        } else if (getArguments() != null) {
            defaultUrl = getArguments().getString(ARG_DEFAULT_URL);
        } else {
            defaultUrl = "";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_start, container, false);
        final EditText urlEditText = (EditText) view.findViewById(R.id.url);
        urlEditText.setText(defaultUrl);
        textWasChanged = false;
        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                textWasChanged = true;
                updateText(s);
            }
        });
        updateText(defaultUrl);
        return view;
    }


    private void updateText(CharSequence text) {
        if(text == null) {
            View view = getView();
            if (view == null) return;
            EditText editText = (EditText) view.findViewById(R.id.url);
            text = editText.getText();
        }
        Uri uri = Uri.parse(text.toString());
        if(mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(ARG_DEFAULT_URL, defaultUrl);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
            updateText(null);
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public void updateDefaultUrl(String defaultUrl) {
        this.defaultUrl = defaultUrl;
        if(!textWasChanged) {
            View view = getView();
            if(view == null) return;
            EditText editText = (EditText) view.findViewById(R.id.url);
            editText.setText(defaultUrl);
            textWasChanged = false; // we changed it
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
