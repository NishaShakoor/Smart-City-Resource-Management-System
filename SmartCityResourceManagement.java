import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.swing.table.DefaultTableModel;



interface Alertable { 
    void sendEmergencyAlert(String message); 
}
interface Reportable { 
    String generateUsageReport();
}

abstract class CityResource {
    protected String resourceID;
    protected String location;
    protected String status;
    protected static int totalResources = 0;

    public CityResource(String resourceID, String location, String status) {
        this.resourceID = resourceID;
        this.location = location;
        this.status = status;
        totalResources++;
    }
    public abstract double calculateMaintenanceCost();
    public String getResourceID() { 
        return resourceID; 
    }
    public String getLocation() { 
        return location; 
    }
    public String getStatus() { 
        return status; 
    }
    public void setStatus(String status) { 
        this.status = status; 
    }
    public static int getTotalResources() { 
        return totalResources; 
    }
    public void onDelete() { 
        totalResources = Math.max(0, totalResources - 1);
    }
    @Override public String toString() {
        return resourceID + " (" + location + ") - " + status;
    }
}



class TransportUnit extends CityResource implements Reportable {
    private String type;
    int capacity;
    int currentPassengers;
    double fuelCost;
    String route;

    public TransportUnit(String resourceID, String location, String status, String type, int capacity, double fuelCost, String route) {
        super(resourceID, location, status);
        this.type = type;
        this.capacity = capacity;
        this.fuelCost = fuelCost;
        this.route = route;
        this.currentPassengers = 0;
    }
    @Override public double calculateMaintenanceCost() { 
        return fuelCost * 0.1 + capacity * 2; 
    }
    @Override public String generateUsageReport() { 
        return "TransportUnit " + resourceID + ": " + currentPassengers + "/" + capacity + " passengers."; 
    }
    public void updatePassengers(int passengers) { 
        this.currentPassengers = Math.max(0, Math.min(passengers, capacity)); }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }
    public int getCurrentPassengers() { return currentPassengers; }
    public String getRoute() { return route; }
    @Override public String toString() { return type + " " + resourceID + " (" + route + ") - " + status; }
}

class PowerStation extends CityResource implements Alertable, Reportable {
    double energyOutput;
    double operationalCost;
    boolean isOperational;
    private static double totalEnergyConsumed = 0;

    public PowerStation(String resourceID, String location, String status, double energyOutput, double operationalCost) {
        super(resourceID, location, status);
        this.energyOutput = energyOutput;
        this.operationalCost = operationalCost;
        this.isOperational = status.equalsIgnoreCase("ACTIVE");
    }
    @Override public double calculateMaintenanceCost() { return operationalCost * 0.15; }
    @Override public void sendEmergencyAlert(String message) { JOptionPane.showMessageDialog(null, "PowerStation " + resourceID + " ALERT: " + message); }
    @Override public String generateUsageReport() { return "PowerStation " + resourceID + ": Output " + energyOutput + " MW, Status: " + status; }
    public double getEnergyOutput() { return energyOutput; }
    public boolean isOperational() { return isOperational; }
    public static double getTotalEnergyConsumed() { return totalEnergyConsumed; }
    public static void resetTotalEnergyConsumed() { totalEnergyConsumed = 0; }
    public void setOperational(boolean operational) { this.isOperational = operational; this.status = operational ? "ACTIVE" : "DOWN"; }
    @Override public String toString() { return "PowerStation " + resourceID + " (" + location + ") - " + status; }
}

class EmergencyService extends CityResource implements Alertable, Reportable {
    private String serviceType;
    int responseTime;
    boolean isAvailable;
    private static double emergencyResponseTime = 0;
    private static int emergencyCount = 0;

    public EmergencyService(String resourceID, String location, String status, String serviceType, int responseTime) {
        super(resourceID, location, status);
        this.serviceType = serviceType;
        this.responseTime = responseTime;
        this.isAvailable = status.equalsIgnoreCase("AVAILABLE");
    }
    @Override public double calculateMaintenanceCost() { return responseTime * 5 + (isAvailable ? 50 : 100); }
    @Override public void sendEmergencyAlert(String message) { JOptionPane.showMessageDialog(null, serviceType + " " + resourceID + " ALERT: " + message); }
    @Override public String generateUsageReport() { return serviceType + " " + resourceID + ": Response time " + responseTime + " min, Status: " + status; }
    public String getServiceType() { return serviceType; }
    public int getResponseTime() { return responseTime; }
    public boolean isAvailable() { return isAvailable; }
    public void setAvailable(boolean available) { this.isAvailable = available; this.status = available ? "AVAILABLE" : "BUSY"; }
    @Override public String toString() { return serviceType + " " + resourceID + " (" + location + ") - " + status; }
}

class CityRepository<T extends CityResource> {
    private List<T> resources = new ArrayList<>();
    public void addResource(T resource) { resources.add(resource); }
    public void removeResource(T resource) { resources.remove(resource); }
    public List<T> getAllResources() { return resources; }
    public T findResourceById(String id) {
        for (T r : resources) if (r.getResourceID().equals(id)) return r;
        return null;
    }
}

class SecurityUtil {
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) { return ""; }
    }
}

class SmartCityGUI extends JFrame {
    private CityRepository<TransportUnit> transportRepo = new CityRepository<>();
    private CityRepository<PowerStation> powerRepo = new CityRepository<>();
    private CityRepository<EmergencyService> emergencyRepo = new CityRepository<>();
    private boolean isAdminMode;
    private JTable resourceTable;
    private DefaultTableModel tableModel;
    private javax.swing.Timer emergencyTimer;
    private JTextField searchField;
    private MapPanel mapPanel;
    private JButton chartBtn;

    public SmartCityGUI(boolean isAdminMode) {
        this.isAdminMode = isAdminMode;
        setTitle("Smart City Resource Management");
        setSize(1100, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        initUI();
        startTransportSimulation();
    }

    private void initUI() {
        JPanel panel = new JPanel(new BorderLayout());
        tableModel = new DefaultTableModel(new Object[]{"ID", "Type", "Location", "Status"}, 0);
        resourceTable = new JTable(tableModel);
        panel.add(new JScrollPane(resourceTable), BorderLayout.CENTER);

        JPanel searchPanel = new JPanel();
        searchField = new JTextField(15);
        JButton searchBtn = new JButton("Search");
        searchBtn.addActionListener(e -> searchResource());
        searchPanel.add(new JLabel("Search by ID/Type:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        panel.add(searchPanel, BorderLayout.NORTH);

        mapPanel = new MapPanel();
        mapPanel.setPreferredSize(new Dimension(300, 0));
        panel.add(mapPanel, BorderLayout.EAST);

        JPanel buttonPanel = new JPanel();
        JButton addBtn = new JButton("Add Resource");
        JButton delBtn = new JButton("Delete Resource");
        JButton editBtn = new JButton("Edit Resource");
        JButton viewBtn = new JButton("View Details");
        JButton reportBtn = new JButton("Generate Reports");
        JButton statusBtn = new JButton("Show System Status");
        JButton emergencyBtn = new JButton("Simulate Emergency");
        JButton updatePassengersBtn = new JButton("Update Passengers");
        chartBtn = new JButton("Show Charts");

        addBtn.addActionListener(e -> addResourceDialog());
        delBtn.addActionListener(e -> deleteResourceDialog());
        editBtn.addActionListener(e -> editResourceDialog());
        viewBtn.addActionListener(e -> viewResourceDialog());
        reportBtn.addActionListener(e -> showReportsDialog());
        statusBtn.addActionListener(e -> showSystemStatus());
        emergencyBtn.addActionListener(e -> simulateEmergency());
        updatePassengersBtn.addActionListener(e -> updatePassengersDialog());

        buttonPanel.add(addBtn); buttonPanel.add(delBtn); buttonPanel.add(editBtn);
        buttonPanel.add(viewBtn); buttonPanel.add(reportBtn); buttonPanel.add(statusBtn);
        buttonPanel.add(emergencyBtn); buttonPanel.add(updatePassengersBtn); buttonPanel.add(chartBtn);

        if (!isAdminMode) {
            addBtn.setEnabled(false); delBtn.setEnabled(false); editBtn.setEnabled(false); updatePassengersBtn.setEnabled(false);
        }

        panel.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(panel);
        refreshTable();
    }

    class MapPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            int w = getWidth(), h = getHeight();
            int margin = 30;
            int total = transportRepo.getAllResources().size() + powerRepo.getAllResources().size() + emergencyRepo.getAllResources().size();
            int yStep = Math.max(40, (h - 2 * margin) / Math.max(1, total));
            int y = margin;
            for (TransportUnit t : transportRepo.getAllResources()) {
                g.setColor(Color.BLUE);
                g.fillOval(margin, y, 20, 20);
                g.drawString(t.getResourceID(), margin + 25, y + 15);
                y += yStep;
            }
            for (PowerStation p : powerRepo.getAllResources()) {
                g.setColor(p.getStatus().equalsIgnoreCase("ACTIVE") ? Color.GREEN : Color.RED);
                g.fillRect(margin + 80, y, 20, 20);
                g.drawString(p.getResourceID(), margin + 105, y + 15);
                y += yStep;
            }
            for (EmergencyService e : emergencyRepo.getAllResources()) {
                g.setColor(Color.ORANGE);
                g.fillOval(margin + 160, y, 20, 20);
                g.drawString(e.getResourceID(), margin + 185, y + 15);
                y += yStep;
            }
        }
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (TransportUnit t : transportRepo.getAllResources())
            tableModel.addRow(new Object[]{t.getResourceID(), "Transport", t.getLocation(), t.getStatus()});
        for (PowerStation p : powerRepo.getAllResources())
            tableModel.addRow(new Object[]{p.getResourceID(), "Power", p.getLocation(), p.getStatus()});
        for (EmergencyService e : emergencyRepo.getAllResources())
            tableModel.addRow(new Object[]{e.getResourceID(), "Emergency", e.getLocation(), e.getStatus()});
        mapPanel.repaint();
    }

    private void searchResource() {
        String query = searchField.getText().trim().toLowerCase();
        tableModel.setRowCount(0);
        for (TransportUnit t : transportRepo.getAllResources()) {
            if (t.getResourceID().toLowerCase().contains(query) || t.getType().toLowerCase().contains(query)) {
                tableModel.addRow(new Object[]{t.getResourceID(), "Transport", t.getLocation(), t.getStatus()});
            }
        }
        for (PowerStation p : powerRepo.getAllResources()) {
            if (p.getResourceID().toLowerCase().contains(query)) {
                tableModel.addRow(new Object[]{p.getResourceID(), "Power", p.getLocation(), p.getStatus()});
            }
        }
        for (EmergencyService e : emergencyRepo.getAllResources()) {
            if (e.getResourceID().toLowerCase().contains(query) || e.getServiceType().toLowerCase().contains(query)) {
                tableModel.addRow(new Object[]{e.getResourceID(), "Emergency", e.getLocation(), e.getStatus()});
            }
        }
        mapPanel.repaint();
    }

    private void addResourceDialog() {
        String[] types = {"Transport", "Power", "Emergency"};
        String type = (String) JOptionPane.showInputDialog(this, "Select resource type:", "Add Resource",
                JOptionPane.PLAIN_MESSAGE, null, types, types[0]);
        if (type == null) return;
        String id = JOptionPane.showInputDialog(this, "Enter Resource ID:");
        String location = JOptionPane.showInputDialog(this, "Enter Location:");
        String status = JOptionPane.showInputDialog(this, "Enter Status:");
        if (type.equals("Transport")) {
            String tType = JOptionPane.showInputDialog(this, "Enter Transport Type (bus/train):");
            int capacity = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter Capacity:"));
            double fuelCost = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter Fuel Cost:"));
            String route = JOptionPane.showInputDialog(this, "Enter Route:");
            transportRepo.addResource(new TransportUnit(id, location, status, tType, capacity, fuelCost, route));
        } else if (type.equals("Power")) {
            double output = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter Energy Output (MW):"));
            double opCost = Double.parseDouble(JOptionPane.showInputDialog(this, "Enter Operational Cost:"));
            powerRepo.addResource(new PowerStation(id, location, status, output, opCost));
        } else if (type.equals("Emergency")) {
            String sType = JOptionPane.showInputDialog(this, "Enter Service Type (police/fire/medical):");
            int respTime = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter Response Time (min):"));
            emergencyRepo.addResource(new EmergencyService(id, location, status, sType, respTime));
        }
        refreshTable();
    }

    private void deleteResourceDialog() {
        String id = JOptionPane.showInputDialog(this, "Enter Resource ID to delete:");
        if (id == null) return;
        deleteResource(id);
        refreshTable();
    }

    private void deleteResource(String resourceId) {
        CityResource resource = transportRepo.findResourceById(resourceId);
        if (resource != null) { resource.onDelete(); transportRepo.removeResource((TransportUnit) resource); return; }
        resource = powerRepo.findResourceById(resourceId);
        if (resource != null) { resource.onDelete(); powerRepo.removeResource((PowerStation) resource); return; }
        resource = emergencyRepo.findResourceById(resourceId);
        if (resource != null) { resource.onDelete(); emergencyRepo.removeResource((EmergencyService) resource); }
    }

    private void editResourceDialog() {
        int row = resourceTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a resource to edit."); return; }
        String id = (String) tableModel.getValueAt(row, 0);
        String type = (String) tableModel.getValueAt(row, 1);
        if (type.equals("Transport")) {
            TransportUnit t = transportRepo.findResourceById(id);
            if (t != null) {
                String newLocation = JOptionPane.showInputDialog(this, "Edit Location:", t.getLocation());
                String newStatus = JOptionPane.showInputDialog(this, "Edit Status:", t.getStatus());
                int newCapacity = Integer.parseInt(JOptionPane.showInputDialog(this, "Edit Capacity:", t.getCapacity()));
                t.location = newLocation; t.status = newStatus; t.capacity = newCapacity;
            }
        } else if (type.equals("Power")) {
            PowerStation p = powerRepo.findResourceById(id);
            if (p != null) {
                String newLocation = JOptionPane.showInputDialog(this, "Edit Location:", p.getLocation());
                String newStatus = JOptionPane.showInputDialog(this, "Edit Status:", p.getStatus());
                double newOutput = Double.parseDouble(JOptionPane.showInputDialog(this, "Edit Output (MW):", p.getEnergyOutput()));
                p.location = newLocation;
                // Resource Dependency: If status set to DOWN, alert all EmergencyServices in same location
                if (!newStatus.equalsIgnoreCase("ACTIVE")) {
                    for (EmergencyService es : emergencyRepo.getAllResources())
                        if (es.getLocation().equalsIgnoreCase(p.getLocation()))
                            es.sendEmergencyAlert("Power outage at " + p.getLocation() + "!");
                }
                p.status = newStatus; p.energyOutput = newOutput;
            }
        } else if (type.equals("Emergency")) {
            EmergencyService e = emergencyRepo.findResourceById(id);
            if (e != null) {
                String newLocation = JOptionPane.showInputDialog(this, "Edit Location:", e.getLocation());
                String newStatus = JOptionPane.showInputDialog(this, "Edit Status:", e.getStatus());
                int newResp = Integer.parseInt(JOptionPane.showInputDialog(this, "Edit Response Time:", e.getResponseTime()));
                e.location = newLocation; e.status = newStatus; e.responseTime = newResp;
            }
        }
        refreshTable();
    }

    private void viewResourceDialog() {
        int row = resourceTable.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Select a resource to view."); return; }
        String id = (String) tableModel.getValueAt(row, 0);
        String type = (String) tableModel.getValueAt(row, 1);
        StringBuilder details = new StringBuilder();
        if (type.equals("Transport")) {
            TransportUnit t = transportRepo.findResourceById(id);
            if (t != null) details.append(t.toString()).append("\nCapacity: ").append(t.getCapacity())
                    .append("\nCurrent Passengers: ").append(t.getCurrentPassengers())
                    .append("\nFuel Cost: ").append(t.fuelCost)
                    .append("\nRoute: ").append(t.getRoute())
                    .append("\nMaintenance Cost: ").append(t.calculateMaintenanceCost());
        } else if (type.equals("Power")) {
            PowerStation p = powerRepo.findResourceById(id);
            if (p != null) details.append(p.toString())
                    .append("\nEnergy Output: ").append(p.getEnergyOutput()).append(" MW")
                    .append("\nOperational Cost: ").append(p.operationalCost)
                    .append("\nMaintenance Cost: ").append(p.calculateMaintenanceCost());
        } else if (type.equals("Emergency")) {
            EmergencyService e = emergencyRepo.findResourceById(id);
            if (e != null) details.append(e.toString())
                    .append("\nResponse Time: ").append(e.getResponseTime()).append(" min")
                    .append("\nMaintenance Cost: ").append(e.calculateMaintenanceCost());
        }
        JOptionPane.showMessageDialog(this, details.toString(), "Resource Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showReportsDialog() {
        StringBuilder sb = new StringBuilder();
        for (TransportUnit t : transportRepo.getAllResources()) sb.append(t.generateUsageReport()).append("\n");
        for (PowerStation p : powerRepo.getAllResources()) sb.append(p.generateUsageReport()).append("\n");
        for (EmergencyService e : emergencyRepo.getAllResources()) sb.append(e.generateUsageReport()).append("\n");
        JOptionPane.showMessageDialog(this, sb.toString(), "Resource Reports", JOptionPane.INFORMATION_MESSAGE);
    }

    private void showSystemStatus() {
        int totalTransports = transportRepo.getAllResources().size();
        int totalPower = powerRepo.getAllResources().size();
        int totalEmergency = emergencyRepo.getAllResources().size();
        int totalPassengers = 0;
        double totalCapacity = 0;
        for (TransportUnit t : transportRepo.getAllResources()) {
            totalPassengers += t.getCurrentPassengers();
            totalCapacity += t.getCapacity();
        }
        double usage = totalCapacity > 0 ? (totalPassengers / totalCapacity) * 100 : 0;
        String msg = "Transports: " + totalTransports + "\nPower Stations: " + totalPower +
                "\nEmergency Services: " + totalEmergency +
                "\nTransport Usage: " + String.format("%.2f", usage) + "%";
        JOptionPane.showMessageDialog(this, msg, "System Status", JOptionPane.INFORMATION_MESSAGE);
    }

    private void simulateEmergency() {
        if (emergencyRepo.getAllResources().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No emergency services available."); return;
        }
        EmergencyService dispatched = emergencyRepo.getAllResources().get(0);
        dispatched.setAvailable(false);
        refreshTable();
        JOptionPane.showMessageDialog(this, "Emergency dispatched: " + dispatched.getResourceID());
        emergencyTimer = new javax.swing.Timer(dispatched.getResponseTime() * 60_000, e -> {
            dispatched.setAvailable(true);
            refreshTable();
            JOptionPane.showMessageDialog(this, "Emergency service " + dispatched.getResourceID() + " is now available.");
        });
        emergencyTimer.setRepeats(false);
        emergencyTimer.start();
    }

    private void updatePassengersDialog() {
        int row = resourceTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a transport resource to update passengers.");
            return;
        }
        String id = (String) tableModel.getValueAt(row, 0);
        String type = (String) tableModel.getValueAt(row, 1);
        if (!type.equals("Transport")) {
            JOptionPane.showMessageDialog(this, "Selected resource is not a transport unit.");
            return;
        }
        TransportUnit t = transportRepo.findResourceById(id);
        if (t != null) {
            int newPassengers = Integer.parseInt(JOptionPane.showInputDialog(this, "Enter new number of passengers (max " + t.getCapacity() + "):", t.getCurrentPassengers()));
            t.updatePassengers(newPassengers);
            refreshTable();
        }
    }

    private void startTransportSimulation() {
        new Thread(() -> {
            Random rand = new Random();
            String[] routes = {"A-B", "B-C", "C-D", "D-E"};
            while (true) {
                for (TransportUnit t : transportRepo.getAllResources()) {
                    t.route = routes[rand.nextInt(routes.length)];
                    t.currentPassengers = rand.nextInt(t.capacity + 1);
                }
                refreshTable();
                try { Thread.sleep(5000); } catch (InterruptedException e) {}
            }
        }).start();
    }
}

public class SmartCityResourceManagement {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String password = JOptionPane.showInputDialog(null, "Enter admin password (or leave blank for user):");
            boolean isAdmin = false;
            if (password != null && !password.isEmpty()) {
                password = password.trim();
                String hash = SecurityUtil.hashPassword(password);
                String adminHash = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
                if (hash.equals(adminHash)) {
                    isAdmin = true;
                } else {
                    JOptionPane.showMessageDialog(null, "Incorrect password. Running in user mode.");
                }
            }
            SmartCityGUI gui = new SmartCityGUI(isAdmin);
            gui.setVisible(true);
        });
    }
}