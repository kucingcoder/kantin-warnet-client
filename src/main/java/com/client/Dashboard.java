/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import model.Data;
import model.Menu;
import model.Pengguna;
import model.Pesanan;

/**
 *
 * @author Hanif
 */
public class Dashboard extends javax.swing.JFrame {
    public static String servernya, portnya;
    public static String sesi = "";
    public static Pengguna akun = null;
    public static ArrayList<Menu> daftar_menu = null;
    public static ArrayList<Pengguna> daftar_pengguna = null;
    public static ArrayList<Item> daftar_pesanan = new ArrayList<>();
    public static ArrayList<Pesanan> daftar_pesanan_pelanggan = new ArrayList<>();
    public static NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    
    public static class Item {
        private final Menu menu;
        private final String catatan;

        public Item(Menu menu, String catatan) {
            this.menu = menu;
            this.catatan = catatan;
        }

        public Menu getMenu() {
            return menu;
        }

        public String getCatatan() {
            return catatan;
        }
    }
    
    Timer PesananSaya = new Timer(30000, e -> {
        UpdateDaftarPesananSaya();
    });
            
    Timer SemuaPesanan = new Timer(30000, e -> {
        UpdateDaftarPesananSemua();
    });
    
    /**
     * Creates new form Dashboard
     */
    public Dashboard() {
        try {
            FileInputStream file = new FileInputStream("kredensial.sesi");
            BufferedReader reader = new BufferedReader(new InputStreamReader(file));

            String kredensial = new String(Base64.getDecoder().decode(reader.readLine()));
            
            String[] baris = kredensial.split("\\n");
            
            reader.close();
            file.close();
            
            servernya = baris[0];
            portnya = baris[1];
            String sesi_tersimpan = baris[2];
            
            Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
            ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

            Data data = new Data("belum diketahui", "cek sesi", null, sesi_tersimpan);

            out.writeObject(data);
            out.flush();

            Data informasi = (Data) in.readObject();
            
            if ("berhasil".equals(informasi.getStatus()) && informasi.getData() != null) {
                akun = (Pengguna) informasi.getData();
                sesi = sesi_tersimpan;
            } else {
                System.out.println("Tidak dapat masuk lewat sesi");
            }
            
            in.close();
            out.close();
            permintaan.close();
        } catch (Exception e) {
            System.out.println("Tidak dapat masuk lewat sesi");
        }
        
        if ("".equals(sesi)) {
            Masuk masuk = new Masuk(this, true);
            masuk.setVisible(true);
        }
        
        if ("".equals(sesi)) { System.exit(0); }
        
        initComponents();
        
        try {
            try {
                FileWriter writer = new FileWriter("kredensial.sesi", false);
                String simpan_sesi = servernya + "\n" + portnya + "\n" + sesi;
                writer.write(Base64.getEncoder().encodeToString(simpan_sesi.getBytes()));
                writer.close();
                
                System.out.println("Sesi telah tersimpan");
            } catch (Exception e) {
                System.out.println("Sesi tidak dapat tersimpan");
            }
            
            if ("pelanggan".equals(akun.getJenis())) {
                tab.remove(kelola_menu);
                tab.remove(kelola_pesanan);
                tab.remove(kelola_pengguna);
                tab.setSelectedIndex(0);
            } else if ("operator".equals(akun.getJenis())) {
                tab.remove(pesan_menu);
                tab.remove(riwayat_pesanan);
                tab.setSelectedIndex(0);
            } else {
                JOptionPane.showMessageDialog(this, "Sesi pengguna tidak valid", "Data", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void UpdateDaftarMenu(JTable tabelnya){
        try {
            Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
            ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

            Data data = new Data("belum diketahui", "daftar menu", null, sesi);

            out.writeObject(data);
            out.flush();

            Data informasi = (Data) in.readObject();

            if (informasi.getData() != null) {
                daftar_menu = (ArrayList<Menu>) informasi.getData();

                DefaultTableModel model = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
                
                model.addColumn("NAMA");
                model.addColumn("HARGA");

                for (Menu menu : daftar_menu) {
                    model.addRow(new Object[]{menu.getNama(), formatRupiah.format(menu.getHarga())});
                }

                tabelnya.setModel(model);
            }

            in.close();
            out.close();
            permintaan.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private void UpdateDaftarOrder(){
        DefaultListModel<String> model = new DefaultListModel<>();
        
        int total_harga = 0;
        
        for(int i = 0; i < daftar_pesanan.size(); i++){
            Menu menu = daftar_pesanan.get(i).getMenu();
            model.add(i, menu.getNama());
            total_harga += menu.getHarga();
        }
        
        order.setModel(model);
        total.setText(formatRupiah.format(total_harga));
    }
    
    private void UpdateDaftarPesananSaya(){
        try {
            Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
            ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

            Data data = new Data("belum diketahui", "riwayat pesanan", akun.getId(), sesi);

            out.writeObject(data);
            out.flush();

            Data informasi = (Data) in.readObject();
            
            if (informasi.getData() != null) {
                ArrayList<Pesanan> riwayat_pesanan = (ArrayList<Pesanan>) informasi.getData();
                
                DefaultTableModel model = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
                
                model.addColumn("MENU");
                model.addColumn("HARGA");
                model.addColumn("CATATAN");
                model.addColumn("STATUS");
                
                for(Pesanan pesanan : riwayat_pesanan){
                    model.addRow(new Object[]{pesanan.getNama_menu(), formatRupiah.format(pesanan.getHarga()), pesanan.getCatatan(), pesanan.getStatus()});
                }
                
                tabel_riwayat_pesanan.setModel(model);
            }
            
            in.close();
            out.close();
            permintaan.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private void UpdateDaftarPesananSemua(){
        try {
            Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
            ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

            Data data = new Data("belum diketahui", "riwayat semua pesanan", null, sesi);

            out.writeObject(data);
            out.flush();

            Data informasi = (Data) in.readObject();
            
            if (informasi.getData() != null) {
                ArrayList<Pesanan> riwayat_pesanan = (ArrayList<Pesanan>) informasi.getData();
                
                DefaultTableModel model = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
                
                model.addColumn("NAMA");
                model.addColumn("MENU");
                model.addColumn("CATATAN");
                model.addColumn("STATUS");
                
                for(Pesanan pesanan : riwayat_pesanan){
                    daftar_pesanan_pelanggan.add(pesanan);
                    model.addRow(new Object[]{pesanan.getNama_pengguna(), pesanan.getNama_menu(), pesanan.getCatatan(), pesanan.getStatus()});
                }
                
                tabel_kelola_pesanan.setModel(model);
            }
            
            in.close();
            out.close();
            permintaan.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    private void UpdateDaftarPengguna(){
        try {
            Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
            ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

            Data data = new Data("belum diketahui", "daftar pengguna", null, sesi);

            out.writeObject(data);
            out.flush();

            Data informasi = (Data) in.readObject();

            if (informasi.getData() != null) {
                daftar_pengguna = (ArrayList<Pengguna>) informasi.getData();

                DefaultTableModel model = new DefaultTableModel() {
                    @Override
                    public boolean isCellEditable(int row, int column) {
                        return false;
                    }
                };
                
                model.addColumn("NAMA");
                model.addColumn("JENIS");

                for (Pengguna pengguna : daftar_pengguna) {
                    model.addRow(new Object[]{pengguna.getNama(), pengguna.getJenis()});
                }

                tabel_kelola_pengguna.setModel(model);
            }

            in.close();
            out.close();
            permintaan.close();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        tab = new javax.swing.JTabbedPane();
        pesan_menu = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tabel_pesan_menu = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        order = new javax.swing.JList<>();
        jLabel6 = new javax.swing.JLabel();
        total = new javax.swing.JLabel();
        PesanMakanan = new javax.swing.JButton();
        hapus_pesanan = new javax.swing.JButton();
        tambah_pesanan = new javax.swing.JButton();
        riwayat_pesanan = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane4 = new javax.swing.JScrollPane();
        tabel_riwayat_pesanan = new javax.swing.JTable();
        next_update_pesanan_saya = new javax.swing.JLabel();
        kelola_pesanan = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane5 = new javax.swing.JScrollPane();
        tabel_kelola_pesanan = new javax.swing.JTable();
        habis = new javax.swing.JButton();
        buat = new javax.swing.JButton();
        selesai = new javax.swing.JButton();
        next_update_pesanan_saya1 = new javax.swing.JLabel();
        kelola_menu = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tabel_kelola_menu = new javax.swing.JTable();
        tambah_menu = new javax.swing.JButton();
        edit_menu = new javax.swing.JButton();
        hapus_menu = new javax.swing.JButton();
        kelola_pengguna = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        tabel_kelola_pengguna = new javax.swing.JTable();
        tambah_pengguna = new javax.swing.JButton();
        edit_pengguna = new javax.swing.JButton();
        hapus_pengguna = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        keluar = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Kantin Warnet");
        setResizable(false);

        tab.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        tab.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                tabStateChanged(evt);
            }
        });

        pesan_menu.setName("pesan menu"); // NOI18N

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel2.setText("Daftar Menu");

        tabel_pesan_menu.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        tabel_pesan_menu.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "NAMA", "HARGA"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tabel_pesan_menu.setRowHeight(25);
        jScrollPane1.setViewportView(tabel_pesan_menu);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel5.setText("Daftar Pesanan");

        order.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane3.setViewportView(order);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel6.setText("Total");

        total.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        total.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        total.setText("0");

        PesanMakanan.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        PesanMakanan.setText("Pesan");
        PesanMakanan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PesanMakananActionPerformed(evt);
            }
        });

        hapus_pesanan.setBackground(new java.awt.Color(255, 0, 0));
        hapus_pesanan.setText("Hapus");
        hapus_pesanan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hapus_pesananActionPerformed(evt);
            }
        });

        tambah_pesanan.setBackground(new java.awt.Color(153, 255, 0));
        tambah_pesanan.setText("Tambah");
        tambah_pesanan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tambah_pesananActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pesan_menuLayout = new javax.swing.GroupLayout(pesan_menu);
        pesan_menu.setLayout(pesan_menuLayout);
        pesan_menuLayout.setHorizontalGroup(
            pesan_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pesan_menuLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(pesan_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 290, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(18, 18, 18)
                .addGroup(pesan_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pesan_menuLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(total, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(PesanMakanan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel5)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pesan_menuLayout.createSequentialGroup()
                        .addComponent(tambah_pesanan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(hapus_pesanan)))
                .addContainerGap(29, Short.MAX_VALUE))
        );
        pesan_menuLayout.setVerticalGroup(
            pesan_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pesan_menuLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(pesan_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel5))
                .addGap(18, 18, 18)
                .addGroup(pesan_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pesan_menuLayout.createSequentialGroup()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 95, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(pesan_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(hapus_pesanan)
                            .addComponent(tambah_pesanan))
                        .addGap(25, 25, 25)
                        .addGroup(pesan_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(total)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(PesanMakanan))
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(25, Short.MAX_VALUE))
        );

        tab.addTab("Pesan Menu", pesan_menu);

        riwayat_pesanan.setName("riwayat pesanan"); // NOI18N

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel7.setText("Pesanan Anda");

        tabel_riwayat_pesanan.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        tabel_riwayat_pesanan.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "MENU", "HARGA", "CATATAN", "STATUS"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tabel_riwayat_pesanan.setRowHeight(25);
        jScrollPane4.setViewportView(tabel_riwayat_pesanan);

        next_update_pesanan_saya.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        next_update_pesanan_saya.setText("diperbaharui tiap 30 detik");

        javax.swing.GroupLayout riwayat_pesananLayout = new javax.swing.GroupLayout(riwayat_pesanan);
        riwayat_pesanan.setLayout(riwayat_pesananLayout);
        riwayat_pesananLayout.setHorizontalGroup(
            riwayat_pesananLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(riwayat_pesananLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(riwayat_pesananLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(riwayat_pesananLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(next_update_pesanan_saya))
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 460, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        riwayat_pesananLayout.setVerticalGroup(
            riwayat_pesananLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(riwayat_pesananLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(riwayat_pesananLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(next_update_pesanan_saya))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );

        tab.addTab("Riwayat Pesanan", riwayat_pesanan);

        kelola_pesanan.setName("kelola pesanan"); // NOI18N

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel8.setText("Daftar Pesanan");

        tabel_kelola_pesanan.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        tabel_kelola_pesanan.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "NAMA", "MENU", "CATATAN", "STATUS"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tabel_kelola_pesanan.setRowHeight(25);
        jScrollPane5.setViewportView(tabel_kelola_pesanan);

        habis.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        habis.setText("Kehabisan");
        habis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                habisActionPerformed(evt);
            }
        });

        buat.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        buat.setText("Buat");
        buat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buatActionPerformed(evt);
            }
        });

        selesai.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        selesai.setText("Selesai");
        selesai.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selesaiActionPerformed(evt);
            }
        });

        next_update_pesanan_saya1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        next_update_pesanan_saya1.setText("diperbaharui tiap 30 detik");

        javax.swing.GroupLayout kelola_pesananLayout = new javax.swing.GroupLayout(kelola_pesanan);
        kelola_pesanan.setLayout(kelola_pesananLayout);
        kelola_pesananLayout.setHorizontalGroup(
            kelola_pesananLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(kelola_pesananLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(kelola_pesananLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(kelola_pesananLayout.createSequentialGroup()
                        .addComponent(habis)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(buat)
                        .addGap(18, 18, 18)
                        .addComponent(selesai))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, kelola_pesananLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(next_update_pesanan_saya1))
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.PREFERRED_SIZE, 460, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        kelola_pesananLayout.setVerticalGroup(
            kelola_pesananLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(kelola_pesananLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(kelola_pesananLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(next_update_pesanan_saya1))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 175, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(kelola_pesananLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(habis)
                    .addComponent(selesai)
                    .addComponent(buat))
                .addGap(23, 23, 23))
        );

        tab.addTab("Kelola Pesanan", kelola_pesanan);

        kelola_menu.setName("kelola menu"); // NOI18N

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setText("Daftar Menu");

        tabel_kelola_menu.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        tabel_kelola_menu.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "NAMA", "HARGA"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tabel_kelola_menu.setRowHeight(25);
        tabel_kelola_menu.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        tabel_kelola_menu.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane2.setViewportView(tabel_kelola_menu);

        tambah_menu.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        tambah_menu.setText("Tambah Baru");
        tambah_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tambah_menuActionPerformed(evt);
            }
        });

        edit_menu.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        edit_menu.setText("Edit");
        edit_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                edit_menuActionPerformed(evt);
            }
        });

        hapus_menu.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        hapus_menu.setText("Hapus");
        hapus_menu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hapus_menuActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout kelola_menuLayout = new javax.swing.GroupLayout(kelola_menu);
        kelola_menu.setLayout(kelola_menuLayout);
        kelola_menuLayout.setHorizontalGroup(
            kelola_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, kelola_menuLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel3)
                .addGap(200, 200, 200))
            .addGroup(kelola_menuLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(kelola_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(kelola_menuLayout.createSequentialGroup()
                        .addComponent(tambah_menu)
                        .addGap(18, 18, 18)
                        .addComponent(edit_menu)
                        .addGap(18, 18, 18)
                        .addComponent(hapus_menu))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 465, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(23, Short.MAX_VALUE))
        );
        kelola_menuLayout.setVerticalGroup(
            kelola_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(kelola_menuLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addComponent(jLabel3)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(kelola_menuLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tambah_menu)
                    .addComponent(edit_menu)
                    .addComponent(hapus_menu))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        tab.addTab("Kelola Menu", kelola_menu);

        kelola_pengguna.setName("kelola pengguna"); // NOI18N

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel4.setText("Daftar Pengguna");

        tabel_kelola_pengguna.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        tabel_kelola_pengguna.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "NAMA", "JENIS"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tabel_kelola_pengguna.setRowHeight(25);
        jScrollPane6.setViewportView(tabel_kelola_pengguna);

        tambah_pengguna.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        tambah_pengguna.setText("Tambah Baru");
        tambah_pengguna.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tambah_penggunaActionPerformed(evt);
            }
        });

        edit_pengguna.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        edit_pengguna.setText("Edit");
        edit_pengguna.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                edit_penggunaActionPerformed(evt);
            }
        });

        hapus_pengguna.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        hapus_pengguna.setText("Hapus");
        hapus_pengguna.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hapus_penggunaActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout kelola_penggunaLayout = new javax.swing.GroupLayout(kelola_pengguna);
        kelola_pengguna.setLayout(kelola_penggunaLayout);
        kelola_penggunaLayout.setHorizontalGroup(
            kelola_penggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, kelola_penggunaLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel4)
                .addGap(176, 176, 176))
            .addGroup(kelola_penggunaLayout.createSequentialGroup()
                .addGap(23, 23, 23)
                .addGroup(kelola_penggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(kelola_penggunaLayout.createSequentialGroup()
                        .addComponent(tambah_pengguna)
                        .addGap(18, 18, 18)
                        .addComponent(edit_pengguna)
                        .addGap(18, 18, 18)
                        .addComponent(hapus_pengguna))
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 465, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(23, Short.MAX_VALUE))
        );
        kelola_penggunaLayout.setVerticalGroup(
            kelola_penggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(kelola_penggunaLayout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addGroup(kelola_penggunaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(tambah_pengguna)
                    .addComponent(edit_pengguna)
                    .addComponent(hapus_pengguna))
                .addContainerGap(21, Short.MAX_VALUE))
        );

        tab.addTab("Kelola Pengguna", kelola_pengguna);

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Kantin Warnet");

        keluar.setBackground(new java.awt.Color(255, 0, 0));
        keluar.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        keluar.setForeground(new java.awt.Color(255, 255, 255));
        keluar.setText("Keluar Akun");
        keluar.setFocusable(false);
        keluar.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                keluarActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(238, 238, 238)
                        .addComponent(keluar))
                    .addComponent(tab, javax.swing.GroupLayout.PREFERRED_SIZE, 515, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(25, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(keluar))
                .addGap(24, 24, 24)
                .addComponent(tab, javax.swing.GroupLayout.PREFERRED_SIZE, 350, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(27, Short.MAX_VALUE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void tabStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_tabStateChanged
        // TODO add your handling code here:
        try {
            String nama_tab = tab.getSelectedComponent().getName();
        
            switch (nama_tab) {
                case "pesan menu":
                    UpdateDaftarMenu(tabel_pesan_menu);
                    PesananSaya.stop();
                    break;

                case "riwayat pesanan":
                    UpdateDaftarPesananSaya();
                    PesananSaya.start();
                    break;

                case "kelola pesanan":
                    UpdateDaftarPesananSemua();
                    SemuaPesanan.start();
                    break;

                case "kelola menu":
                    UpdateDaftarMenu(tabel_kelola_menu);
                    SemuaPesanan.stop();
                    break;

                case "kelola pengguna":
                    UpdateDaftarPengguna();
                    SemuaPesanan.stop();
                    break;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Terjadi kesalahan di sisi pengguna", "Sistem", JOptionPane.ERROR_MESSAGE);
            System.out.println(e.getMessage());
        }
    }//GEN-LAST:event_tabStateChanged

    private void tambah_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tambah_menuActionPerformed
        // TODO add your handling code here:
        TambahMenu dialog = new TambahMenu(this, true);
        dialog.setVisible(true);
        UpdateDaftarMenu(tabel_kelola_menu);
    }//GEN-LAST:event_tambah_menuActionPerformed

    private void edit_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_edit_menuActionPerformed
        // TODO add your handling code here:
        if (tabel_kelola_menu.getSelectedRow() == -1) { return; }
        
        Menu info = daftar_menu.get(tabel_kelola_menu.getSelectedRow());
        
        EditMenu dialog = new EditMenu(this, true);
        dialog.setInfo(info.getId(), info.getNama(), info.getHarga());
        dialog.showInfo();
        dialog.setVisible(true);
        UpdateDaftarMenu(tabel_kelola_menu);
    }//GEN-LAST:event_edit_menuActionPerformed

    private void hapus_menuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hapus_menuActionPerformed
        // TODO add your handling code here:
        if (tabel_kelola_menu.getSelectedRow() == -1) { return; }
        
        try {
            Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
            ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

            Data data = new Data("belum diketahui", "hapus menu", daftar_menu.get(tabel_kelola_menu.getSelectedRow()).getId(), sesi);

            out.writeObject(data);
            out.flush();

            Data informasi = (Data) in.readObject();
            
            if ("berhasil".equals(informasi.getStatus())) {
                JOptionPane.showMessageDialog(this, "Berhasil menghapus menu", "Hapus Menu", JOptionPane.INFORMATION_MESSAGE);
                UpdateDaftarMenu(tabel_kelola_menu);
            } else {
                JOptionPane.showMessageDialog(this, "Gagal menghapus menu", "Hapus Menu", JOptionPane.ERROR_MESSAGE);
            }
            
            in.close();
            out.close();
            permintaan.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menghapus menu", "Hapus Menu", JOptionPane.ERROR_MESSAGE);
            System.out.println(e.getMessage());
        }
    }//GEN-LAST:event_hapus_menuActionPerformed

    private void tambah_penggunaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tambah_penggunaActionPerformed
        // TODO add your handling code here:
        TambahPengguna dialog = new TambahPengguna(this, true);
        dialog.setVisible(true);
        UpdateDaftarPengguna();
    }//GEN-LAST:event_tambah_penggunaActionPerformed

    private void edit_penggunaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_edit_penggunaActionPerformed
        // TODO add your handling code here:
        if (tabel_kelola_pengguna.getSelectedRow() == -1) { return; }
        
        Pengguna info = daftar_pengguna.get(tabel_kelola_pengguna.getSelectedRow());
        
        EditPengguna dialog = new EditPengguna(this, true);
        dialog.setInfo(info.getId(), info.getNama(), info.getJenis());
        dialog.showInfo();
        dialog.setVisible(true);
        UpdateDaftarPengguna();
    }//GEN-LAST:event_edit_penggunaActionPerformed

    private void hapus_penggunaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hapus_penggunaActionPerformed
        // TODO add your handling code here:
        if (tabel_kelola_pengguna.getSelectedRow() == -1) { return; }
        
        try {
            Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
            ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

            Data data = new Data("belum diketahui", "hapus pengguna", daftar_pengguna.get(tabel_kelola_pengguna.getSelectedRow()).getId(), sesi);

            out.writeObject(data);
            out.flush();

            Data informasi = (Data) in.readObject();
            
            if ("berhasil".equals(informasi.getStatus())) {
                JOptionPane.showMessageDialog(this, "Berhasil menghapus pengguna", "Hapus Pengguna", JOptionPane.INFORMATION_MESSAGE);
                UpdateDaftarPengguna();
            } else {
                JOptionPane.showMessageDialog(this, "Gagal menghapus pengguna", "Hapus Pengguna", JOptionPane.ERROR_MESSAGE);
            }
            
            in.close();
            out.close();
            permintaan.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menghapus pengguna", "Hapus Pengguna", JOptionPane.ERROR_MESSAGE);
            System.out.println(e.getMessage());
        }
    }//GEN-LAST:event_hapus_penggunaActionPerformed

    private void keluarActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_keluarActionPerformed
        // TODO add your handling code here:
        try {
            Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
            ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

            Data data = new Data("belum diketahui", "hapus sesi", null, sesi);

            out.writeObject(data);
            out.flush();

            Data informasi = (Data) in.readObject();

            if ("berhasil".equals(informasi.getStatus())) {
                File file = new File("kredensial.sesi");
                if (file.exists()) { file.delete(); }
                JOptionPane.showMessageDialog(this, "Berhasil kaluar akun\nSILAHKAN JALANKAN KEMBALI PROGRAMNYA", "Keluar Akun", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            } else {
                JOptionPane.showMessageDialog(this, "Gagal keluar akun", "Keluar Akun", JOptionPane.ERROR_MESSAGE);
            }
            
            in.close();
            out.close();
            permintaan.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal keluar akun", "Keluar Akun", JOptionPane.ERROR_MESSAGE);
            System.out.println(e.getMessage());
        }
    }//GEN-LAST:event_keluarActionPerformed

    private void tambah_pesananActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_tambah_pesananActionPerformed
        // TODO add your handling code here:
        TambahPesanan dialog = new TambahPesanan(this, true);
        dialog.setVisible(true);
        UpdateDaftarOrder();
    }//GEN-LAST:event_tambah_pesananActionPerformed

    private void hapus_pesananActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hapus_pesananActionPerformed
        // TODO add your handling code here:
        if (order.getSelectedIndex() == -1) {
            return;
        }
        
        try {
            daftar_pesanan.remove(order.getSelectedIndex());
            UpdateDaftarOrder();
            JOptionPane.showMessageDialog(this, "Berhasil menghapus item di pesanan", "Hapus Item Pesanan", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menghapus item di pesanan", "Hapus Item Pesanan", JOptionPane.ERROR_MESSAGE);
            System.out.println(e.getMessage());
        }
    }//GEN-LAST:event_hapus_pesananActionPerformed

    private void PesanMakananActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PesanMakananActionPerformed
        // TODO add your handling code here:
        if (daftar_pesanan.size() == 0) {
            return;
        }
        
        try {
            Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
            ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());
            
            ArrayList<Pesanan> daftar_orderan = new ArrayList<>();
            
            for(Item item : daftar_pesanan){
                Pesanan pesanan = new Pesanan(akun.getId(), item.getMenu().getId(), item.getCatatan());
                daftar_orderan.add(pesanan);
            }

            Data data = new Data("belum diketahui", "buat pesanan", daftar_orderan, sesi);

            out.writeObject(data);
            out.flush();

            Data informasi = (Data) in.readObject();
            
            if ("berhasil".equals(informasi.getStatus())) {
                daftar_pesanan.clear();
                UpdateDaftarOrder();
                JOptionPane.showMessageDialog(this, "Berhasil memesan menu", "Pesan Menu", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Gagal memesan menu", "Pesan Menu", JOptionPane.ERROR_MESSAGE);
                System.out.println(informasi.getStatus());
            }
            
            in.close();
            out.close();
            permintaan.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memesan menu", "Pesan Menu", JOptionPane.ERROR_MESSAGE);
            System.out.println(e.getMessage());
        }
    }//GEN-LAST:event_PesanMakananActionPerformed

    private void habisActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_habisActionPerformed
        // TODO add your handling code here:
        int index = tabel_kelola_pesanan.getSelectedRow();
        DefaultTableModel model = (DefaultTableModel) tabel_kelola_pesanan.getModel();
        if (index > -1 && "Dipesan".equals(model.getValueAt(index, 3))) {
            try {
                Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
                ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

                Pesanan pesanan = new Pesanan(daftar_pesanan_pelanggan.get(index).getId(), daftar_pesanan_pelanggan.get(index).getId_pengguna(), daftar_pesanan_pelanggan.get(index).getId_menu(), "Sudah Habis (MAAF)");

                Data data = new Data("belum diketahui", "update pesanan", pesanan, sesi);

                out.writeObject(data);
                out.flush();

                Data informasi = (Data) in.readObject();

                if ("berhasil".equals(informasi.getStatus())) {
                    UpdateDaftarPesananSemua();
                    JOptionPane.showMessageDialog(this, "Berhasil memberitahu bahwa menu habis", "Update Pesanan", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal memberitahu bahwa menu habis", "Update Pesanan", JOptionPane.ERROR_MESSAGE);
                    System.out.println(informasi.getStatus());
                }

                in.close();
                out.close();
                permintaan.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal memberitahu bahwa menu habis", "Update Pesanan", JOptionPane.ERROR_MESSAGE);
                System.out.println(e.getMessage());
            }
        }
    }//GEN-LAST:event_habisActionPerformed

    private void buatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buatActionPerformed
        // TODO add your handling code here:
        int index = tabel_kelola_pesanan.getSelectedRow();
        DefaultTableModel model = (DefaultTableModel) tabel_kelola_pesanan.getModel();
        if (index > -1 && "Dipesan".equals(model.getValueAt(index, 3))) {
            try {
                Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
                ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

                Pesanan pesanan = new Pesanan(daftar_pesanan_pelanggan.get(index).getId(), daftar_pesanan_pelanggan.get(index).getId_pengguna(), daftar_pesanan_pelanggan.get(index).getId_menu(), "Sedang Dibuat");

                Data data = new Data("belum diketahui", "update pesanan", pesanan, sesi);

                out.writeObject(data);
                out.flush();

                Data informasi = (Data) in.readObject();

                if ("berhasil".equals(informasi.getStatus())) {
                    UpdateDaftarPesananSemua();
                    JOptionPane.showMessageDialog(this, "Berhasil memberitahu bahwa menu sedang dibuat", "Update Pesanan", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal memberitahu bahwa menu sedang dibuat", "Update Pesanan", JOptionPane.ERROR_MESSAGE);
                    System.out.println(informasi.getStatus());
                }

                in.close();
                out.close();
                permintaan.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal memberitahu bahwa menu sedang dibuat", "Update Pesanan", JOptionPane.ERROR_MESSAGE);
                System.out.println(e.getMessage());
            }
        }
    }//GEN-LAST:event_buatActionPerformed

    private void selesaiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selesaiActionPerformed
        // TODO add your handling code here:
        int index = tabel_kelola_pesanan.getSelectedRow();
        DefaultTableModel model = (DefaultTableModel) tabel_kelola_pesanan.getModel();
        if (index > -1 && "Sedang Dibuat".equals(model.getValueAt(index, 3))) {
            try {
                Socket permintaan = new Socket(servernya, Integer.parseInt(portnya));
                ObjectOutputStream out = new ObjectOutputStream(permintaan.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(permintaan.getInputStream());

                Pesanan pesanan = new Pesanan(daftar_pesanan_pelanggan.get(index).getId(), daftar_pesanan_pelanggan.get(index).getId_pengguna(), daftar_pesanan_pelanggan.get(index).getId_menu(), "Selesai");

                Data data = new Data("belum diketahui", "update pesanan", pesanan, sesi);

                out.writeObject(data);
                out.flush();

                Data informasi = (Data) in.readObject();

                if ("berhasil".equals(informasi.getStatus())) {
                    UpdateDaftarPesananSemua();
                    JOptionPane.showMessageDialog(this, "Berhasil memberitahu bahwa menu selesai dibuat", "Update Pesanan", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal memberitahu bahwa menu selesai dibuat", "Update Pesanan", JOptionPane.ERROR_MESSAGE);
                    System.out.println(informasi.getStatus());
                }

                in.close();
                out.close();
                permintaan.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal memberitahu bahwa menu selesai dibuat", "Update Pesanan", JOptionPane.ERROR_MESSAGE);
                System.out.println(e.getMessage());
            }
        }
    }//GEN-LAST:event_selesaiActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton PesanMakanan;
    private javax.swing.JButton buat;
    private javax.swing.JButton edit_menu;
    private javax.swing.JButton edit_pengguna;
    private javax.swing.JButton habis;
    private javax.swing.JButton hapus_menu;
    private javax.swing.JButton hapus_pengguna;
    private javax.swing.JButton hapus_pesanan;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JPanel kelola_menu;
    private javax.swing.JPanel kelola_pengguna;
    private javax.swing.JPanel kelola_pesanan;
    private javax.swing.JButton keluar;
    private javax.swing.JLabel next_update_pesanan_saya;
    private javax.swing.JLabel next_update_pesanan_saya1;
    private javax.swing.JList<String> order;
    private javax.swing.JPanel pesan_menu;
    private javax.swing.JPanel riwayat_pesanan;
    private javax.swing.JButton selesai;
    private javax.swing.JTabbedPane tab;
    public static javax.swing.JTable tabel_kelola_menu;
    private javax.swing.JTable tabel_kelola_pengguna;
    private javax.swing.JTable tabel_kelola_pesanan;
    private javax.swing.JTable tabel_pesan_menu;
    private javax.swing.JTable tabel_riwayat_pesanan;
    private javax.swing.JButton tambah_menu;
    private javax.swing.JButton tambah_pengguna;
    private javax.swing.JButton tambah_pesanan;
    private javax.swing.JLabel total;
    // End of variables declaration//GEN-END:variables
}