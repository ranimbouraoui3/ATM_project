package packageLecteur;

import com.sun.javacard.apduio.CadT1Client;
import com.sun.javacard.apduio.CadTransportException;
import java.util.Arrays;
import java.util.Scanner;
import com.sun.javacard.apduio.Apdu;
import java.net.Socket;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class Lecteur {
    private static final byte CLA_MONAPPLET = (byte) 0xB0;
    private static final byte INS_DEPOSER = 0x01;
    private static final byte INS_RETIRER = 0x02;
    private static final byte INS_CONSULTER_SOLDE = 0x03;
    private static final byte INS_VERIFIER_PIN = 0x04;
    private static final byte INS_CHANGER_PIN = 0x05;
    private static final byte INS_CONSULTER_HIS = 0x06;

    static CadT1Client cad;

    public void connecter() {
        Socket sckCarte;
        try {
            sckCarte = new Socket("localhost", 9025);
            sckCarte.setTcpNoDelay(true);
            BufferedInputStream input = new BufferedInputStream(sckCarte.getInputStream());
            BufferedOutputStream output = new BufferedOutputStream(sckCarte.getOutputStream());
            cad = new CadT1Client(input, output);
        } catch (Exception e) {
            System.out.println("erreur de connection");
            return;
        }
    }

    public void MiseTension() {
        try {
            cad.powerUp();
        } catch (Exception e) {
            System.out.println("erreur de mise tension");
            return;
        }
    }

    public void HorsTension() {
        try {
            cad.powerDown();
        } catch (Exception e) {
            System.out.println("erreur de horstenstion");
            return;
        }
    }

    public void select() {
        Apdu apdu = new Apdu();
        apdu.command[Apdu.CLA] = 0x00;
        apdu.command[Apdu.INS] = (byte) 0xA4;
        apdu.command[Apdu.P1] = 0x04;
        apdu.command[Apdu.P2] = 0x00;
        byte[] appletAID = { 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x00, 0x00 };
        apdu.setDataIn(appletAID);

        try {
            cad.exchangeApdu(apdu);
        } catch (IOException | CadTransportException e) {
            e.printStackTrace();
        }
        if (apdu.getStatus() != 0x9000) {
            System.out.println("erreur de selection");
            System.exit(1);
        }
    }

    public int interroger() {
        Apdu apdu = new Apdu();
        apdu.command[Apdu.CLA] = CLA_MONAPPLET;
        apdu.command[Apdu.P1] = 0x00;
        apdu.command[Apdu.P2] = 0x00;
        apdu.setLe(0x7f);
        apdu.command[Apdu.INS] = INS_CONSULTER_SOLDE;

        int solde = -1;
        try {
            cad.exchangeApdu(apdu);
        } catch (IOException | CadTransportException e3) {
            e3.printStackTrace();
        }
        if (apdu.getStatus() != 0x9000) {
            System.out.println("erreur");
        } else {
            solde = (apdu.dataOut[0] << 8) | (apdu.dataOut[1] & 0xFF);
        }
        return solde;
    }

    public void depot(int montant) {
        Apdu apdu = new Apdu();
        apdu.command[Apdu.CLA] = CLA_MONAPPLET;
        apdu.command[Apdu.P1] = 0x00;
        apdu.command[Apdu.P2] = 0x00;
        apdu.setLe(0x7f);
        apdu.setLc(2);
        apdu.dataIn = new byte[2];
        apdu.dataIn[0] = (byte) ((montant >> 8) & 0xFF);
        apdu.dataIn[1] = (byte) (montant & 0xFF);
        apdu.command[Apdu.INS] = INS_DEPOSER;
        try {
            cad.exchangeApdu(apdu);
        } catch (IOException | CadTransportException e3) {
            e3.printStackTrace();
        }
        if (apdu.getStatus() != 0x9000) {
            System.out.println("Erreur : status word different de 0x9000");
        }
    }

    public void retrait(int montant) {
        Apdu apdu = new Apdu();
        apdu.command[Apdu.CLA] = CLA_MONAPPLET;
        apdu.command[Apdu.P1] = 0x00;
        apdu.command[Apdu.P2] = 0x00;
        apdu.setLe(0x7f);
        apdu.setLc(2);
        apdu.command[Apdu.INS] = INS_RETIRER;

        apdu.dataIn = new byte[2];
        apdu.dataIn[0] = (byte) ((montant >> 8) & 0xFF);
        apdu.dataIn[1] = (byte) (montant & 0xFF);

        try {
            cad.exchangeApdu(apdu);
        } catch (IOException | CadTransportException e3) {
            e3.printStackTrace();
        }
        if (apdu.getStatus() != 0x9000) {
            System.out.println("Erreur : status word different de 0x9000");
        }
    }

    public boolean entrePin(String pin) {
        Apdu apdu = new Apdu();
        apdu.command[Apdu.CLA] = CLA_MONAPPLET;
        apdu.command[Apdu.P1] = 0x00;
        apdu.command[Apdu.P2] = 0x00;
        apdu.setLe(0x7f);
        apdu.setLc(0x04);
        apdu.command[Apdu.INS] = INS_VERIFIER_PIN;

        byte[] pinBytes = new byte[pin.length()];
        for (int i = 0; i < pin.length(); i++) {
            int pinInt = Character.getNumericValue(pin.charAt(i));
            pinBytes[i] = (byte) pinInt;
        }

        apdu.dataIn = new byte[pinBytes.length];
        System.out.println();
        for (int i = 0; i < pinBytes.length; i++) {
            apdu.dataIn[i] = pinBytes[i];
        }
        try {
            cad.exchangeApdu(apdu);
            if (apdu.getStatus() != 0x9000) {
                return false;
            }
        } catch (IOException | CadTransportException e3) {
            e3.printStackTrace();
            return false;
        }

        return true;
    }

    public boolean changerPin(String pin) {
        Apdu apdu = new Apdu();
        apdu.command[Apdu.CLA] = CLA_MONAPPLET;
        apdu.command[Apdu.P1] = 0x00;
        apdu.command[Apdu.P2] = 0x00;
        apdu.setLe(0x7f);
        apdu.setLc(0x04);
        apdu.command[Apdu.INS] = INS_CHANGER_PIN;

        byte[] pinBytes = new byte[pin.length()];
        for (int i = 0; i < pin.length(); i++) {
            int pinInt = Character.getNumericValue(pin.charAt(i));
            pinBytes[i] = (byte) pinInt;
        }

        apdu.dataIn = new byte[pin.length()];
        for (int i = 0; i < pinBytes.length; i++) {
            apdu.dataIn[i] = pinBytes[i];
        }
        try {
            cad.exchangeApdu(apdu);
            if (apdu.getStatus() != 0x9000) {
                return false;
            }
        } catch (IOException | CadTransportException e3) {
            e3.printStackTrace();
        }
        return true;
    }

    public void affiche() {
        Apdu apdu = new Apdu();
        apdu.command[Apdu.CLA] = CLA_MONAPPLET;
        apdu.command[Apdu.P1] = 0x00;
        apdu.command[Apdu.P2] = 0x00;
        apdu.setLe(0x7f);
        apdu.setLc(0);
        apdu.command[Apdu.INS] = INS_CONSULTER_HIS;

        try {
            cad.exchangeApdu(apdu);
        } catch (IOException | CadTransportException e) {
            e.printStackTrace();
        }
        if (apdu.getStatus() != 0x9000) {
            System.out.println("erreur ");
        } else {
            for (int i = 0; i < apdu.dataOut.length; i++) {
                System.out.println(apdu.dataOut[i]);
            }
        }
    }

    public String[] historique() {
        Apdu apdu = new Apdu();
        apdu.command[Apdu.CLA] = CLA_MONAPPLET;
        apdu.command[Apdu.P1] = 0x00;
        apdu.command[Apdu.P2] = 0x00;
        apdu.setLe(0x7f);
        apdu.setLc(0);
        apdu.command[Apdu.INS] = INS_CONSULTER_HIS;

        try {
            cad.exchangeApdu(apdu);
        } catch (IOException | CadTransportException e) {
            e.printStackTrace();
            return null;
        }
        if (apdu.getStatus() != 0x9000) {
        } else {
            String[] his = new String[10];
            int offset = 0;
            String msg = "";
            int i = 0;
            if (apdu.dataOut == null || apdu.dataOut.length == 0) {
                his[0] = "Aucune transaction trouvée.";
                return his;
            }
            while (offset < apdu.dataOut.length) {
                short id = (short) (((apdu.dataOut[offset] & 0xFF) << 8) | (apdu.dataOut[offset + 1] & 0xFF));
                offset += 2;
                int asciipremierEl = apdu.dataOut[offset];

                String type = "";
                if (asciipremierEl == 68) {
                    type = "Depot";
                    offset += 5;
                } else if (asciipremierEl == 82) {
                    type = "Retrait";
                    offset += 7;
                }
                short montant = (short) (((apdu.dataOut[offset] & 0xFF) << 8) | (apdu.dataOut[offset + 1] & 0xFF));
                offset += 2;
                msg = "Transaction ID : " + id + "\nType : " + type + "\nMontant : " + montant + "\n-------------------------";
                his[i] = msg;
                i++;
            }
            return his;
        }
        return null;
    }
}
