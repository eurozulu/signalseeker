package org.spoofer.signalseeker;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import org.spoofer.signalseeker.celldb.Cell;
import org.spoofer.signalseeker.celldb.CellDatabase;
import org.spoofer.signalseeker.celldb.CellResources;
import org.spoofer.signalseeker.location.LocationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FirstFragment extends Fragment implements LocationService.LocationServiceListener {
    private final Handler guiHandler = new Handler(Looper.getMainLooper());

    private CellResources cellResources;
    private String countryCode;
    private AtomicBoolean isFetching = new AtomicBoolean();

    private TextView txtLocation;
    private TextView txtCoords;
    private TextView txtCellInfo;

    private final List<Cell> localCells = new ArrayList<>();
    private Cell selectedCell = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_first, container, false);
        txtLocation = view.findViewById(R.id.textview_location);
        txtCoords = view.findViewById(R.id.textview_coordinates);
        txtCellInfo = view.findViewById(R.id.textview_cell_info);
        return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(FirstFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });

        cellResources = new CellResources(getContext());
    }


    @Override
    public void LocationUpdate(final Location location) {
        try {
            Geocoder geocoder = new Geocoder(getContext());
            List<Address> addrs = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addrs.size() == 0) {
                return;
            }
            countryCode = addrs.get(0).getCountryCode();

            final String addr = readAddress(addrs.get(0));
            guiHandler.post(new Runnable() {
                @Override
                public void run() {
                    txtLocation.setText(addr);
                    if (txtCoords.getVisibility() == View.VISIBLE) {
                        txtCoords.setText(String.format("Latitude: %f  Longitude: %f", location.getLatitude(), location.getLongitude()));
                    }
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to geocode location", Toast.LENGTH_LONG).show();
            return;
        }

        // if database for current country not present, download in background thread.
        // When download completes, it calls the updateClosestCells
        if (!cellResources.hasDatabase(countryCode)) {
            if (!isFetching.getAndSet(true))
                fetchDatabase(location);
        } else {
            guiHandler.post(new Runnable() {
                @Override
                public void run() {
                    updateClosestCells(location);
                }
            });
        }
    }

    private void setSelectedCell() {
        if (selectedCell == null) {
            txtCellInfo.setText("waiting for location...");
            return;
        }
        
        Cell cell = localCells.get(selected);
        txtCellInfo.setText(String.format("%s (%s)  Distance: %d km", cell.getCellID(), cell.getMobileNetworkCode(), cell.getDistance()));
    }

    private void updateClosestCells(final Location location) {
        CellDatabase db;
        try {
            db = cellResources.getDatabase(countryCode);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), String.format("Failed to open cell database %s", e.toString()), Toast.LENGTH_LONG).show();
            return;
        }

        List<Cell> locCells = db.findLocalCells(location);
        localCells.clear();
        localCells.addAll(locCells);

        // Old selection no longer valid, select the closest.
        if (getSelectedCellIndex() < 0) {
            selectedCell = !localCells.isEmpty() ? localCells.get(0) : null;
        }
        setSelectedCell();
    }

    private int getSelectedCellIndex() {
        if (selectedCell == null)
            return -1;
        for (int i = 0; i < localCells.size(); i++) {
            Cell cell = localCells.get(i);
            if (cell.getCellID().equalsIgnoreCase(selectedCell.getCellID())) {
                return i;
            }
        }
        return -1;
    }

    private void fetchDatabase(final Location location) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                guiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(),
                                String.format("downloading cell database for %s", countryCode),
                                Toast.LENGTH_LONG).show();
                    }
                });

                try {
                    cellResources.downloadDatabase(countryCode);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(getContext(),
                            String.format("downloading cell database for %s failed %s", countryCode, e.toString()),
                            Toast.LENGTH_LONG).show();
                }

                guiHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updateClosestCells(location);
                    }
                });

                isFetching.set(false);
            }
        }).start();
    }

    private String readAddress(Address addr) {
        StringBuilder s = new StringBuilder();

        int count = addr.getMaxAddressLineIndex();
        for (int i = 0; i <= count; i++) {
            if (addr.getAddressLine(i) != null) {
                s.append(addr.getAddressLine(i));
                s.append('\n');
            }
        }
        if (addr.getPostalCode() != null) {
            s.append(addr.getPostalCode());
            s.append('\n');
        }
        return s.toString();
    }

}