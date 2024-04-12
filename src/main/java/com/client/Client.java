/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.client;

import com.formdev.flatlaf.FlatIntelliJLaf;

/**
 *
 * @author Hanif
 */
public class Client {

    public static void main(String[] args) {
        FlatIntelliJLaf.setup();
        Dashboard dashboard = new Dashboard();
        dashboard.setVisible(true);
    }
}