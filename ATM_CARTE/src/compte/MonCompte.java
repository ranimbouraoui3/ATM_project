package compte;

import javacard.framework.APDU;
import javacard.framework.Applet;
import javacard.framework.ISOException;
import javacard.framework.ISO7816;

import javacard.framework.*;

//byte 1 octet
//short 2 octets
public class MonCompte extends Applet {
	
    private static final byte CLA_MONAPPLET = (byte) 0xB0;
    private static final byte INS_DEPOSER = 0x01;
    private static final byte INS_RETIRER = 0x02;
    private static final byte INS_CONSULTER_SOLDE = 0x03;
    private static final byte INS_VERIFIER_PIN = 0x04;
    private static final byte INS_CHANGER_PIN = 0x05;
    private static final byte INS_CONSULTER_HIS = 0x06;
    private static final byte MAX_PIN_TRIES = 3;
    
    private static final short SW_PIN_VERIFICATION_REQUIRED = 0x6300;
    private OwnerPIN pin;
    private short solde;
    private static final short MAX_HISTORY_SIZE = 10;  // Taille maximale de l'historique
    private Transaction[] historique;  // Tableau pour stocker les transactions
    private short historyIndex; 
    
    private static class Transaction {
        byte[] type;
        short montant;
        short ID;

        private static short conteurID = 0;
        public Transaction(byte[] type, short montant) {
            this.type = type;
            this.montant = montant;
            this.ID = ++conteurID; 
        }
    }

    private MonCompte() {
        pin = new OwnerPIN(MAX_PIN_TRIES, (byte) 4);
        pin.update(new byte[]{0,0,0,0}, (short) 0, (byte) 4); // on va mettre le pin par defaut 0000 
        //mais on va creer une methode pour changer le code pin
        solde = 0;
        historique = new Transaction[MAX_HISTORY_SIZE];
        historyIndex = 0;
        register();
    }

    public static void install(byte bArray[], short bOffset, byte bLength) throws ISOException {
        new MonCompte();
    }

    public void process(APDU apdu) throws ISOException {
        byte[] buffer = apdu.getBuffer();

        if (selectingApplet()) return;

        if (buffer[ISO7816.OFFSET_CLA] != CLA_MONAPPLET) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        
        //si le pin n'est pas encore valider et l'utilisateur esseye de faire autre chose que verifier le pin alors il tombe dans une exception 
        if (!pin.isValidated() && buffer[ISO7816.OFFSET_INS] != INS_VERIFIER_PIN) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }

        switch (buffer[ISO7816.OFFSET_INS]) {
            case INS_VERIFIER_PIN:
                verifyPin(apdu);
                
                break;
            case INS_DEPOSER:
                deposer(apdu);
                break;
            case INS_RETIRER:
                retirer(apdu);
                break;
            case INS_CONSULTER_SOLDE:
                consulterSolde(apdu);
                break;
            case INS_CHANGER_PIN:
            	changerPin(apdu);
                break;
            case INS_CONSULTER_HIS:
            	consulterHistorique(apdu);
                break;
            default:
                ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void verifyPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        byte pinLength = buffer[ISO7816.OFFSET_LC];
        apdu.setIncomingAndReceive();
        if (pin.check(buffer, ISO7816.OFFSET_CDATA, pinLength)) {
            return;
        }
        // la verification de MAX_PIN_TRIES est fait automatiquemnet par OwnerPIN
        ISOException.throwIt(ISO7816.SW_WRONG_DATA);
    }
    
    private void deposer(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        apdu.setIncomingAndReceive();
        short montant = Util.getShort(buffer, ISO7816.OFFSET_CDATA);       
        short somme = (short)(solde + montant);
        solde = somme;
        ajouterTransaction(new byte[] {'D', 'e', 'p', 'o', 't'}, montant);
        
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) 0); 
        ISOException.throwIt(ISO7816.SW_NO_ERROR);
    }
    
    private void retirer(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short dataLength = apdu.setIncomingAndReceive();
        if (dataLength != 2) {
            ISOException.throwIt((short) 0x6F01); 
        }
        short montant = Util.getShort(buffer, ISO7816.OFFSET_CDATA);
        if (montant < 0) {
            ISOException.throwIt((short) 0x6F02);
        }

        if (montant > solde) {
            ISOException.throwIt((short) 0x6F03); 
        }
        solde -= montant;
        ajouterTransaction(new byte[] { 'R', 'e', 't', 'r', 'a', 'i', 't' }, montant);
        
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) 0); 
        ISOException.throwIt(ISO7816.SW_NO_ERROR);
    }


    private void consulterSolde(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        //puisque la valeur des solde est divier sur 2 octet 
        buffer[0] = (byte) (solde >> 8); 
        buffer[1] = (byte) (solde & 0xFF); 
        apdu.setOutgoingAndSend((short) 0, (short) 2);
        //le lecteur va reconstruire la valeur 
    }
    
    private void changerPin(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        if (!pin.isValidated()) {
            ISOException.throwIt(SW_PIN_VERIFICATION_REQUIRED);
        }
         
        short dataLength = apdu.setIncomingAndReceive();
        if (dataLength != 4) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        pin.update(buffer, ISO7816.OFFSET_CDATA, (byte) dataLength);
        
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) 1);
        buffer[0] = (byte) 0x90;
        apdu.sendBytes((short) 0, (short) 1);
        
    }
    private void ajouterTransaction(byte[] type, short montant) {
        if (historyIndex < MAX_HISTORY_SIZE) {
            historique[historyIndex] = new Transaction(type, montant);
            historyIndex++;
        } else {
            for (short i = 1; i < MAX_HISTORY_SIZE; i++) {
            	historique[(short)(i - 1)] = historique[(short)i];
            }
            historique[MAX_HISTORY_SIZE - 1] = new Transaction(type, montant);
        }
    }
    private void consulterHistorique(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        short totalLength = 0;

        for (short i = 0; i < historyIndex; i++) {
            Transaction t = historique[i];
            totalLength += 2; //ID (short)
            totalLength += t.type.length; // type
            totalLength += 2; // montant (short)
        }
        
        apdu.setOutgoing();
        apdu.setOutgoingLength(totalLength);
        
        short offset = 0; 
        for (short i = 0; i < historyIndex; i++) {
            Transaction t = historique[i];            
            buffer[offset] = (byte) (t.ID >> 8); 
            buffer[(short) (offset + 1)] = (byte) (t.ID & 0xFF); 
            offset += 2;
            Util.arrayCopyNonAtomic(t.type, (short) 0, buffer, offset, (short) t.type.length);
            offset += t.type.length;
            buffer[offset] = (byte) (t.montant >> 8); 
            buffer[(short) (offset + 1)] = (byte) (t.montant & 0xFF); 
            offset += 2;
        }       
        apdu.sendBytes((short) 0, totalLength);
    }
}
