package io.ristretto.decaptcha;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
    private static final String ARG_URL = "url";
    private final static String TAG = "StartFragment";

    private Uri uri;
    private boolean uriWasChanged = false;

    private OnFragmentInteractionListener mListener;

    public StartFragment() {
        // Required empty public constructor
    }

    public static StartFragment newInstance(String url) {
        StartFragment fragment = new StartFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_URL, Uri.parse(url));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            uri = savedInstanceState.getParcelable(ARG_URL);
        } else if (getArguments() != null) {
            uri = getArguments().getParcelable(ARG_URL);
        }
        if(uri == null) {
            Log.w(TAG, "No URI set");
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("https");
            uri = builder.build();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_start, container, false);
        final EditText urlEditText = (EditText) view.findViewById(R.id.url);
        urlEditText.setText(uri.toString());
        uriWasChanged = false;
        urlEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                uriWasChanged = true;
                setUri(s);
            }
        });
        return view;
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ARG_URL, uri);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
            mListener.onURLChanged(uri);
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

    /**
     * Update the default url
     * @param defaultUrl the default url
     */
    public void updateDefaultUrl(String defaultUrl) {
        if(!uriWasChanged) {
            this.uri = Uri.parse(defaultUrl);
            View view = getView();
            if(view == null) return;
            EditText editText = (EditText) view.findViewById(R.id.url);
            editText.setText(defaultUrl);
            uriWasChanged = false; // we changed it
        }
    }

    public void setUri(Editable uriFromText) {
        uri = Uri.parse(uriFromText.toString());
        if (mListener != null) {
            mListener.onURLChanged(uri);
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onURLChanged(Uri uri);
    }
}
