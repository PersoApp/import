package de.persoapp.android.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

import de.persoapp.android.R;

/**
 * @author Ralf Wondratschek
 *
 * TODO: Ausweis an Smartphone halten? Um die Auswahl einzuschränken
 *
 */
public class PinOptionsActivity extends AbstractNfcActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_options);

        AbsListView adapterView = (AbsListView) findViewById(R.id.adapterView);
        adapterView.setAdapter(mAdapter);
        adapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        startActivity(new Intent(PinOptionsActivity.this, ActivatePinActivity.class));
                        break;

                    case 1:
                        startActivity(new Intent(PinOptionsActivity.this, ChangePinActivity.class));
                        break;
                }
            }
        });
    }

    private BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public String getItem(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.activate_npa);
                case 1:
                    return getString(R.string.change_pin);
                case 2:
                    return getString(R.string.unlock_pin);
                default:
                    throw new IndexOutOfBoundsException();
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @SuppressWarnings("ConstantConditions")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.pin_options_list_item, parent, false);
            }

            TextView label = (TextView) convertView.findViewById(R.id.textView);
            label.setText(getItem(position));

            return convertView;
        }
    };
}
