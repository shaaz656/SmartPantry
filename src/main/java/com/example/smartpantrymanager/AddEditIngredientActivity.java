package com.example.smartpantrymanager;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
public class AddEditIngredientActivity extends AppCompatActivity {

    private static final String[] UNITS = {"g", "kg", "ml", "l", "tsp", "tbsp", "cup", "pcs"};

    private DatabaseHelper db;
    private AutoCompleteTextView editName;
    private EditText editQuantity, editExpiry;
    private Spinner spinnerUnit;
    private long itemId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = new DatabaseHelper(this);
        editName = findViewById(R.id.editName);
        editQuantity = findViewById(R.id.editQuantity);
        editExpiry = findViewById(R.id.editExpiry);
        spinnerUnit = findViewById(R.id.spinnerUnit);
        Button buttonSave = findViewById(R.id.buttonSave);

        spinnerUnit.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, UNITS));

        // Suggest ingredient names used by the recipes; picking one also sets the unit.
        final Map<String, String> known = db.getKnownIngredients();
        editName.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, new ArrayList<>(known.keySet())));
        editName.setOnItemClickListener((parent, view, position, id) -> {
            String unit = known.get(parent.getItemAtPosition(position).toString());
            for (int i = 0; i < UNITS.length; i++) {
                if (UNITS[i].equals(unit)) spinnerUnit.setSelection(i);
            }
        });

        // Did MainActivity send us an item_id? Then we are editing.
        itemId = getIntent().getLongExtra("item_id", -1);
        if (itemId != -1) {
            setTitle("Edit Ingredient");
            PantryItem item = db.getItem(itemId);
            if (item != null) {
                editName.setText(item.getName());
                editName.dismissDropDown();
                editQuantity.setText(item.formatQuantity());
                editExpiry.setText(item.getExpiry());
                for (int i = 0; i < UNITS.length; i++) {
                    if (UNITS[i].equals(item.getUnit())) spinnerUnit.setSelection(i);
                }
            }
        } else {
            setTitle("Add Ingredient");
        }

        editExpiry.setOnClickListener(v -> showDatePicker());
        editExpiry.setOnLongClickListener(v -> {
            editExpiry.setText("");
            return true;
        });
        buttonSave.setOnClickListener(v -> save());
    }

    private void showDatePicker() {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, day) ->
                editExpiry.setText(String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day)),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void save() {
        String name = editName.getText().toString().trim();
        String qtyText = editQuantity.getText().toString().trim();

        // ---- input validation ----
        if (name.isEmpty()) {
            editName.setError("Please enter an ingredient name");
            return;
        }
        if (!name.matches(".*[A-Za-z].*")) {
            editName.setError("Name must contain letters");
            return;
        }
        if (qtyText.isEmpty()) {
            editQuantity.setError("Please enter a quantity");
            return;
        }
        double quantity;
        try {
            quantity = Double.parseDouble(qtyText);
        } catch (NumberFormatException e) {
            editQuantity.setError("Enter a valid number");
            return;
        }
        if (quantity <= 0) {
            editQuantity.setError("Quantity must be greater than 0");
            return;
        }
        String unit = spinnerUnit.getSelectedItem().toString();
        String expiry = editExpiry.getText().toString().trim();

        if (itemId == -1) {
            db.insertItem(name, quantity, unit, expiry);
            Toast.makeText(this, "Ingredient added", Toast.LENGTH_SHORT).show();
        } else {
            db.updateItem(itemId, name, quantity, unit, expiry);
            Toast.makeText(this, "Ingredient updated", Toast.LENGTH_SHORT).show();
        }
        finish(); // go back; MainActivity.onResume() refreshes the list
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}