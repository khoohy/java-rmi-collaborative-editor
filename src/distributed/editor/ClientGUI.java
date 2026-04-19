package distributed.editor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ClientGUI extends JFrame {
    private DocumentService service;
    private final String clientId = "User_" + (int)(Math.random()*1000);
    private final JPanel mainPanel = new JPanel();
    private long localVersion = 0;
    private final Map<Integer, String> localCache = new HashMap<>();

    public ClientGUI(String ip) {
        try {
            Registry reg = LocateRegistry.getRegistry(ip, 1099);
            service = (DocumentService) reg.lookup("DocService");
            
            setTitle("Editor - " + clientId);
            
            JButton addBtn = new JButton("+ Add New Line");
            addBtn.setFocusPainted(false);
            addBtn.addActionListener(e -> {
                try {
                    service.addLine(clientId);
                    updateLocalVersionAndRefresh();
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            
            getContentPane().add(addBtn, BorderLayout.NORTH);
            getContentPane().add(new JScrollPane(mainPanel), BorderLayout.CENTER);
            
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    try { service.releaseAllLocks(clientId); } catch (Exception ex) {}
                }
            });

            refreshUI();
            startPolling();
            
            setSize(650, 600);
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Connection Error: " + e.getMessage());
        }
    }

    private void refreshUI() {
        try {
            List<String> serverLines = service.getDocumentLines();
            Map<Integer, String> activeLocks = service.getActiveLocks();
            
            mainPanel.removeAll();
            for (int i = 0; i < serverLines.size(); i++) {
                int idx = i;
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT));
                
                String owner = activeLocks.get(idx);
                String textToDisplay = clientId.equals(owner) ? 
                                       localCache.getOrDefault(idx, serverLines.get(i)) : 
                                       serverLines.get(i);
                
                JTextField txt = new JTextField(textToDisplay, 25);
                JButton btnAction = new JButton();
                JButton btnDelete = new JButton("Delete");

                if (clientId.equals(owner)) {
                    txt.setEditable(true); txt.setBackground(new Color(255, 255, 200)); 
                    btnAction.setText("Save");
                    btnDelete.setEnabled(false);
                } else if (owner != null) {
                    txt.setEditable(false); txt.setBackground(Color.LIGHT_GRAY); 
                    btnAction.setText("Occupied"); btnAction.setEnabled(false);
                    btnDelete.setEnabled(false);
                } else {
                    txt.setEditable(false); txt.setBackground(Color.WHITE); 
                    btnAction.setText("Edit"); btnAction.setEnabled(true);
                    btnDelete.setEnabled(true);
                }

                txt.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        if (txt.isEditable()) { localCache.put(idx, txt.getText()); }
                    }
                });

                btnAction.addActionListener(e -> {
                    try {
                        if (btnAction.getText().equals("Edit")) {
                            if (service.requestLock(idx, clientId)) {
                                localCache.put(idx, txt.getText());
                                updateLocalVersionAndRefresh();
                            } else {
                                JOptionPane.showMessageDialog(this, "Line already occupied!");
                            }
                        } else if (btnAction.getText().equals("Save")) {
                            service.updateLine(idx, txt.getText(), clientId);
                            service.releaseLock(idx, clientId);
                            localCache.remove(idx);
                            updateLocalVersionAndRefresh();
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                });

                btnDelete.addActionListener(e -> {
                    try {
                        // RE-ADDED POPUP: Confirmation before deletion
                        int response = JOptionPane.showConfirmDialog(this, 
                                "Are you sure you want to delete this line?", 
                                "Confirm Delete", 
                                JOptionPane.YES_NO_OPTION);
                        
                        if (response == JOptionPane.YES_OPTION) {
                            if (service.deleteLine(idx)) {
                                updateLocalVersionAndRefresh();
                            } else {
                                JOptionPane.showMessageDialog(this, "Cannot delete - Line is locked!");
                            }
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                });

                row.add(txt); row.add(btnAction); row.add(btnDelete);
                mainPanel.add(row);
            }
            mainPanel.revalidate();
            mainPanel.repaint();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void updateLocalVersionAndRefresh() throws Exception {
        localVersion = service.getLastModified();
        refreshUI();
    }

    private void startPolling() {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); 
                    long serverVer = service.getLastModified();
                    if (serverVer > localVersion) {
                        localVersion = serverVer;
                        SwingUtilities.invokeLater(this::refreshUI);
                    }
                } catch (Exception e) {}
            }
        }).start();
    }

    public static void main(String[] args) {
        String ip = JOptionPane.showInputDialog("Enter Server IP Address:");
        if (ip != null) new ClientGUI(ip);
    }
}