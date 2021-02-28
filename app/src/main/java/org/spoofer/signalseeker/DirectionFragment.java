package org.spoofer.signalseeker;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import org.spoofer.signalseeker.celldb.Cell;
import org.spoofer.signalseeker.location.CellLocationService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DirectionFragment extends Fragment implements CellLocationService.CellLocationListener {

    private TextView txtLocation;
    private TextView txtCoords;
    private TextView txtCellInfo;
    private ImageButton btnPrevCell;
    private ImageButton btnNextCell;
    private ImageView imgArrow;

    private final List<Cell> localCells = new ArrayList<>();
    private Cell selectedCell = null;
    private Location lastLocation = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_direction, container, false);
        txtLocation = view.findViewById(R.id.textview_location);
        txtCoords = view.findViewById(R.id.textview_coordinates);
        txtCellInfo = view.findViewById(R.id.textview_cell_info);
        btnPrevCell = view.findViewById(R.id.img_button_prev);
        btnNextCell = view.findViewById(R.id.img_button_next);
        imgArrow = view.findViewById(R.id.image_view_arrow);
        return view;
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(DirectionFragment.this)
                        .navigate(R.id.action_FirstFragment_to_SecondFragment);
            }
        });
    }

    private void updateLocationDisplay() {
        if (lastLocation == null) {
            txtLocation.setText("waiting for location...");
            return;
        }
        Address addr = getAddressFromLocation(lastLocation);
        if (addr == null) {
            return;
        }
        txtLocation.setText(getAddressText(addr));
    }

    private void updateCellDisplay() {
        // Update selected cell gui
        int sel = getSelectedCellIndex();
        btnPrevCell.setEnabled(sel > 0);
        btnNextCell.setEnabled(sel >= 0 && sel < (localCells.size() - 2));

        if (selectedCell == null) {
            txtCellInfo.setText("waiting for location...");
        } else {
            txtCellInfo.setText(String.format("%s (%s)  Distance: %d km",
                    selectedCell.getCellID(), selectedCell.getMobileNetworkCode(), selectedCell.getDistance()));
        }

        float degree = 0;
        if (selectedCell != null && lastLocation != null) {
            Location cellLocation = new Location("");
            cellLocation.setLatitude(selectedCell.getLatitude());
            cellLocation.setLongitude(selectedCell.getLongitude());
            degree = lastLocation.bearingTo(cellLocation);
        }
        imgArrow.setRotation(degree);
    }

    private int getSelectedCellIndex() {
        if (selectedCell == null || localCells.isEmpty())
            return -1;
        for (int i = 0; i < localCells.size(); i++) {
            Cell cell = localCells.get(i);
            if (cell.getCellID().equalsIgnoreCase(selectedCell.getCellID())) {
                return i;
            }
        }
        return -1;
    }

    private Address getAddressFromLocation(Location location) {
        Address addr;
        try {
            Geocoder geocoder = new Geocoder(getContext());
            List<Address> addrs = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addrs.size() == 0) {
                return null;
            }
            addr = addrs.get(0);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to geocode location", Toast.LENGTH_LONG).show();
            return null;
        }
        return addr;
    }

    private String getAddressText(Address addr) {
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


    /* ============ CellLocationListener interface ================= */


    @Override
    public void LocationUpdate(Location location) {
        lastLocation = location;
        updateLocationDisplay();
    }

    @Override
    public void LocalCellsUpdate(List<Cell> cells) {
        localCells.clear();
        localCells.addAll(cells);

        // if old selection no longer valid, select the closest.
        if (getSelectedCellIndex() < 0) {
            selectedCell = !localCells.isEmpty() ? localCells.get(0) : null;
        }
        updateCellDisplay();
    }

}