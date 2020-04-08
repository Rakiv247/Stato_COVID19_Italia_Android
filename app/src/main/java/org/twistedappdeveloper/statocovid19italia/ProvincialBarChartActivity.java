package org.twistedappdeveloper.statocovid19italia;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.MPPointF;

import org.twistedappdeveloper.statocovid19italia.adapters.ProvinceAdapter;
import org.twistedappdeveloper.statocovid19italia.datastorage.DataStorage;
import org.twistedappdeveloper.statocovid19italia.model.ProvinceSelection;
import org.twistedappdeveloper.statocovid19italia.model.ProvinceSelectionWrapper;
import org.twistedappdeveloper.statocovid19italia.model.TrendInfo;
import org.twistedappdeveloper.statocovid19italia.model.TrendValue;
import org.twistedappdeveloper.statocovid19italia.utils.TrendUtils;
import org.twistedappdeveloper.statocovid19italia.utils.Utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;

public class ProvincialBarChartActivity extends AppCompatActivity implements View.OnClickListener {

    private BarChart chart;
    private DataStorage dataStorage;
    private TextView txtMarkerData;

    private ImageButton btnIndietro;
    private ImageButton btnAvanti;
    private ImageButton btnChangeOrder;

    private int cursore, dataLen;

    private String selectedTrendKey;

    private List<String> selectedProvince;

    private Map<String, DataStorage> dataStorageMap = new HashMap<>();

    private Map<String, List<ProvinceSelection>> provinceListMap;

    private String[] trendsKey;
    private String[] trendsName;
    private int checkedItem;

    private boolean orderTrend = false;

    public static final int MIN_ELEMENTS = 3;
    public static final int MAX_ELEMENTS = 30;

    private Map<String, TrendValue> currentValues = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bar_chart);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Button btnPercentage = findViewById(R.id.btnPercentage);
        btnPercentage.setVisibility(View.GONE);

        dataStorage = DataStorage.getIstance();
        dataLen = dataStorage.getDataStorageByDataContext(dataStorage.getSubLevelDataKeys().get(0)).getDataLength();

        cursore = getIntent().getIntExtra(Utils.CURSORE_KEY, dataLen - 1);
        selectedTrendKey = getIntent().getStringExtra(Utils.TREND_KEY);
        selectedProvince = new ArrayList<>();
        Collections.addAll(selectedProvince, getIntent().getStringArrayExtra(Utils.PROVINCE_ARRAY_KEY));

        for (String regione : dataStorage.getSubLevelDataKeys()) {
            for (String provincia : dataStorage.getDataStorageByDataContext(regione).getSubLevelDataKeys()) {
                if (!provincia.equals("In fase di definizione/aggiornamento")) {
                    dataStorageMap.put(provincia, dataStorage.getDataStorageByDataContext(regione).getDataStorageByDataContext(provincia));
                }
            }
        }

        List<TrendInfo> trendInfoListTmp = dataStorageMap.values().iterator().next().getTrendsList();
        Collections.sort(trendInfoListTmp);
        trendsName = new String[trendInfoListTmp.size()];
        trendsKey = new String[trendInfoListTmp.size()];
        for (int i = 0; i < trendInfoListTmp.size(); i++) {
            trendsKey[i] = trendInfoListTmp.get(i).getKey();
            trendsName[i] = trendInfoListTmp.get(i).getName();
            if (trendsKey[i].equals(selectedTrendKey)) {
                checkedItem = i;
            }
        }

        txtMarkerData = findViewById(R.id.txtMarkerData);
        chart = findViewById(R.id.barChart);
        btnIndietro = findViewById(R.id.btnIndietro);
        btnAvanti = findViewById(R.id.btnAvanti);
        ImageButton btnProvince = findViewById(R.id.btnProvinciale);
        ImageButton btnCambiaMisura = findViewById(R.id.btnCambiaMisura);
        btnChangeOrder = findViewById(R.id.btnChangeOrder);
        ImageButton btnChangeDate = findViewById(R.id.btnChangeDate);

        btnIndietro.setOnClickListener(this);
        btnAvanti.setOnClickListener(this);
        btnProvince.setOnClickListener(this);
        btnCambiaMisura.setOnClickListener(this);
        btnChangeOrder.setOnClickListener(this);
        btnPercentage.setOnClickListener(this);
        btnChangeDate.setOnClickListener(this);

        chart.setTouchEnabled(true);
        chart.setBackgroundColor(Color.WHITE);
        chart.setDrawBarShadow(false);
        chart.setDrawValueAboveBar(true);
        chart.setPinchZoom(false);
        chart.setDoubleTapToZoomEnabled(false);
        chart.setDrawGridBackground(false);
        chart.getDescription().setEnabled(false);
        chart.setHighlightFullBarEnabled(false);

        chart.getAxisLeft().setEnabled(false);
        chart.getAxisLeft().setAxisMinimum(0);
        chart.getAxisRight().setAxisMinimum(0);

        BarChartActivityMaker maker = new BarChartActivityMaker(getApplicationContext());
        chart.setMarker(maker);

        ValueFormatter xAxisFormatter = new ProvinceFormatter();
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(MAX_ELEMENTS);
        xAxis.setValueFormatter(xAxisFormatter);
        xAxis.setLabelRotationAngle(90);

        Legend l = chart.getLegend();
        l.setForm(Legend.LegendForm.SQUARE);
        l.setTextSize(12f);
        l.setTextColor(Color.BLACK);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.TOP);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setXOffset(-5f);

        calcolaProvinceListMap();


        btnEnableStatusCheck();

        setData();
    }

    private void calcolaProvinceListMap() {
        provinceListMap = new HashMap<>();
        for (String regione : dataStorage.getSubLevelDataKeys()) {
            for (String provincia : dataStorage.getDataStorageByDataContext(regione).getSubLevelDataKeys()) {
                if (!provincia.equals("In fase di definizione/aggiornamento")) {
                    List<ProvinceSelection> provinceSelections;
                    if (provinceListMap.containsKey(regione)) {
                        provinceSelections = provinceListMap.get(regione);
                    } else {
                        provinceSelections = new ArrayList<>();
                        provinceListMap.put(regione, provinceSelections);
                    }
                    provinceSelections.add(new ProvinceSelection(provincia, selectedProvince.contains(provincia)));
                }
            }
        }
        for (List<ProvinceSelection> provinceSelections : provinceListMap.values()) {
            Collections.sort(provinceSelections);
        }
    }


    private void setData() {
        currentValues.clear();
        ArrayList<BarEntry> values = new ArrayList<>();

        boolean isMinimumZero = true;

        int i = 0;
        for (String selectedProvincia : selectedProvince) {
            TrendValue trendValue = dataStorageMap.get(selectedProvincia).getTrendByKey(selectedTrendKey).getTrendValues().get(cursore);
            currentValues.put(selectedProvincia, trendValue);
            int value = trendValue.getValue();
            values.add(new BarEntry(i++, value, selectedProvincia));
            txtMarkerData.setText(String.format(getString(R.string.dati_relativi_al), trendValue.getDate()));
            if (value < 0) {
                isMinimumZero = false;
            }
        }
        if (orderTrend) {
            Utils.quickSort(values, 0, values.size() - 1);
        }

        BarDataSet barDataSet;
        barDataSet = new BarDataSet(values, TrendUtils.getTrendNameByTrendKey(getApplicationContext().getResources(), selectedTrendKey));
        barDataSet.setDrawIcons(false);
        barDataSet.setColor(TrendUtils.getColorByTrendKey(ProvincialBarChartActivity.this, selectedTrendKey));

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(barDataSet);

        if (isMinimumZero) {
            chart.getAxisLeft().setAxisMinimum(0);
            chart.getAxisRight().setAxisMinimum(0);
        } else {
            chart.getAxisLeft().resetAxisMinimum();
            chart.getAxisRight().resetAxisMinimum();
        }

        BarData data = new BarData(dataSets);
        data.setValueTextSize(9f);
//        data.setBarWidth(0.9f);

        chart.setData(data);
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.animateY(200);

        checkBarValueVisualization();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnAvanti:
                if (cursore < dataStorage.getDataLength()) {
                    cursore++;
                }
                btnEnableStatusCheck();
                setData();
                break;
            case R.id.btnIndietro:
                if (cursore > 0) {
                    cursore--;
                }
                btnEnableStatusCheck();
                setData();
                break;
            case R.id.btnCambiaMisura:
                AlertDialog.Builder builder = new AlertDialog.Builder(ProvincialBarChartActivity.this);

                builder.setSingleChoiceItems(trendsName, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        selectedTrendKey = trendsKey[which];
                        setData();
                        checkedItem = which;
                    }
                });
                builder.setTitle(getResources().getString(R.string.sel_misura));
                AlertDialog alert = builder.create();
                alert.show();
                break;
            case R.id.btnProvinciale:
                calcolaProvinceListMap();
                final Dialog dialog = new Dialog(ProvincialBarChartActivity.this, R.style.AppAlert);
                dialog.setContentView(R.layout.dialog_province);

                final ListView listViewProvince = dialog.findViewById(R.id.listViewDialogProvince);
                final Button btnOk = dialog.findViewById(R.id.btnCloseTrendDialog);
                final Button btnDeselectAllTrends = dialog.findViewById(R.id.btnDeselectAll);

                final List<ProvinceSelectionWrapper> provinceSelectionWrappers = new ArrayList<>();
                for (String regione : provinceListMap.keySet()) {
                    provinceSelectionWrappers.add(new ProvinceSelectionWrapper(regione, provinceListMap.get(regione)));
                }
                Collections.sort(provinceSelectionWrappers);

                TextView textView = dialog.findViewById(R.id.txtProvinceDialogTitle);
                textView.setText(String.format("%s (%s sel.)", getString(R.string.province_da_visualizzare), numberOfSelectedElement()));
                final ProvinceAdapter provinceAdapter = new ProvinceAdapter(ProvincialBarChartActivity.this, R.layout.list_province, provinceSelectionWrappers, textView);
                listViewProvince.setAdapter(provinceAdapter);

                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        switch (v.getId()) {
                            case R.id.btnCloseTrendDialog:
                                if (numberOfSelectedElement() < MIN_ELEMENTS || numberOfSelectedElement() > MAX_ELEMENTS) {
                                    Toast.makeText(ProvincialBarChartActivity.this, String.format(getString(R.string.limite_selezione), MIN_ELEMENTS, MAX_ELEMENTS), Toast.LENGTH_LONG).show();
                                } else {
                                    selectedProvince = new ArrayList<>();
                                    for (List<ProvinceSelection> provinceList : provinceListMap.values()) {
                                        for (ProvinceSelection provinceSelection : provinceList) {
                                            if (provinceSelection.isSelected()) {
                                                selectedProvince.add(provinceSelection.getProvincia());
                                            }
                                        }
                                    }
                                    setData();
                                    dialog.dismiss();
                                }
                                break;
                            case R.id.btnDeselectAll:
                                for (List<ProvinceSelection> provinceList : provinceListMap.values()) {
                                    for (ProvinceSelection provinceSelection : provinceList) {
                                        provinceSelection.setSelected(false);
                                    }
                                }
                                provinceAdapter.notifyDataSetChanged();
                                break;
                        }
                    }
                };

                btnOk.setOnClickListener(clickListener);
                btnDeselectAllTrends.setOnClickListener(clickListener);
                dialog.show();
                break;
            case R.id.btnChangeOrder:
                orderTrend = !orderTrend;
                if (!orderTrend) {
                    btnChangeOrder.setImageResource(R.drawable.baseline_bar_chart_white_24);
                } else {
                    btnChangeOrder.setImageResource(R.drawable.baseline_signal_cellular_alt_white_24);
                }
                setData();
                break;
            case R.id.btnChangeDate:
                try {
                    String minDataS = dataStorage.getFullDateStringByIndex(0);
                    String maxDataS = dataStorage.getFullDateStringByIndex(dataStorage.getDataLength() - 1);
                    SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ITALY);
                    Date minData = dateFormat.parse(minDataS);
                    Date maxData = dateFormat.parse(maxDataS);

                    final Calendar calendar = Calendar.getInstance();
                    calendar.setTime(dataStorage.getDateByIndex(cursore));
                    DatePickerDialog datePickerDialog = new DatePickerDialog(ProvincialBarChartActivity.this,
                            new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                                    calendar.set(Calendar.YEAR, year);
                                    calendar.set(Calendar.MONTH, month);
                                    calendar.set(Calendar.DAY_OF_MONTH, day);
                                    cursore = dataStorage.getIndexByDate(calendar.getTime());
                                    btnEnableStatusCheck();
                                    setData();
                                }
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

                    datePickerDialog.getDatePicker().setMinDate(minData.getTime());
                    datePickerDialog.getDatePicker().setMaxDate(maxData.getTime());
                    datePickerDialog.show();
                } catch (ParseException e) {
                    e.printStackTrace();
                    Toast.makeText(ProvincialBarChartActivity.this, "Non è possibile selezionare un data", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private int numberOfSelectedElement() {
        int n = 0;
        for (List<ProvinceSelection> provinceList : provinceListMap.values()) {
            for (ProvinceSelection provinceSelection : provinceList) {
                if (provinceSelection.isSelected()) {
                    n++;
                }
            }
        }
        return n;
    }


    private void btnEnableStatusCheck() {
        if (cursore > 0) {
            btnIndietro.setEnabled(true);
            btnIndietro.setImageResource(R.drawable.baseline_keyboard_backspace_white_24);
        } else {
            btnIndietro.setEnabled(false);
            btnIndietro.setImageResource(R.drawable.baseline_keyboard_backspace_gray_24);
        }

        if (cursore < dataLen - 1) {
            btnAvanti.setEnabled(true);
            btnAvanti.setImageResource(R.drawable.baseline_keyboard_white_24);
        } else {
            btnAvanti.setEnabled(false);
            btnAvanti.setImageResource(R.drawable.baseline_keyboard_gray_24);
        }
    }

    private void checkBarValueVisualization() {
        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == ORIENTATION_LANDSCAPE) {
            chart.getData().setDrawValues(true);
        } else {
            chart.getData().setDrawValues(false);
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        checkBarValueVisualization();
    }

    private class ProvinceFormatter extends ValueFormatter {
        private static final int maxLen = 10;

        @Override
        public String getFormattedValue(float value) {
            IBarDataSet dataSetByIndex = chart.getData().getDataSetByIndex(0);
            if (value >= dataSetByIndex.getEntryCount()) {
                return "";
            }
            BarEntry barEntry = dataSetByIndex.getEntryForIndex((int) value);
            String nomeRegione = barEntry.getData().toString();
            if (nomeRegione.length() > maxLen) {
                return String.format("%s.", nomeRegione.substring(0, getMaxLength(nomeRegione)));
            } else {
                return nomeRegione.substring(0, getMaxLength(nomeRegione));
            }
        }

        private int getMaxLength(String nomeRegione) {
            return Math.min(maxLen, nomeRegione.length());
        }
    }

    public class BarChartActivityMaker extends MarkerView {

        private TextView txtBarMarkerTitle, txtBarMarkerCurrentValue, txtBarPrecValue, txtBarMarkerVariazione;

        public BarChartActivityMaker(Context context) {
            super(context, R.layout.bar_chart_marker);
            txtBarMarkerTitle = findViewById(R.id.txtBarMarkerTitle);
            txtBarMarkerCurrentValue = findViewById(R.id.txtBarMarkerCurrentValue);
            txtBarPrecValue = findViewById(R.id.txtBarPrecValue);
            txtBarMarkerVariazione = findViewById(R.id.txtBarMarkerVariazione);
            findViewById(R.id.txtBarMarkerPercentage).setVisibility(GONE);
            findViewById(R.id.txtBarMarkerPercentageTitle).setVisibility(GONE);
        }

        @Override
        public MPPointF getOffset() {
            super.getOffset().x = -getWidth();
            super.getOffset().y = -getHeight();
            return super.getOffset();
        }

        @Override
        public void refreshContent(Entry e, Highlight highlight) {
            int position = (int) e.getX();
            String dataContext = chart.getData().getDataSetByIndex(0).getEntryForIndex(position).getData().toString();
            txtBarMarkerTitle.setText(dataContext);

            TrendValue trendValue = currentValues.get(dataContext);
            txtBarMarkerCurrentValue.setText(String.format("%s", trendValue.getValue()));
            txtBarPrecValue.setText(String.format("%s", trendValue.getPrecValue()));
            txtBarMarkerVariazione.setText(String.format("%s", trendValue.getDelta()));
            int color = TrendUtils.getColorByTrendKey(getApplicationContext(), selectedTrendKey);
            txtBarMarkerCurrentValue.setTextColor(color);
            txtBarPrecValue.setTextColor(color);
            txtBarMarkerVariazione.setTextColor(color);

            super.refreshContent(e, highlight);
        }
    }
}
