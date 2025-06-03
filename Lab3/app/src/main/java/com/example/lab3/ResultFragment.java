package com.example.lab3;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// Фрагмент для відображення результату вибору
public class ResultFragment extends Fragment {

    private static final String ARG_RESULT_TEXT = "result_text";

    // Створення нового екземпляру з передачею тексту результату
    public static ResultFragment newInstance(String resultText) {
        ResultFragment fragment = new ResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RESULT_TEXT, resultText);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_result, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView textResult = view.findViewById(R.id.text_result);
        Button buttonCancel = view.findViewById(R.id.button_cancel);

        // Отримання та виведення результату
        String resultText = getArguments() != null ? getArguments().getString(ARG_RESULT_TEXT) : "";
        textResult.setText(resultText);

        // Обробка кнопки "Очистити" (видалити фрагмент результату)
        buttonCancel.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();

            // Приховування роздільної лінії та контейнера
            requireActivity().findViewById(R.id.result_fragment_container).setVisibility(View.GONE);
            requireActivity().findViewById(R.id.separator_line).setVisibility(View.GONE);

            // Скидання форми
            InputFragment inputFragment = (InputFragment)
                    getParentFragmentManager().findFragmentById(R.id.input_fragment_container);
            if (inputFragment != null) {
                inputFragment.resetForm();
            }
        });
    }
}
