package packageLecteur;

public class Main {

    public static void main(String[] args) {
        Lecteur lecteur = new Lecteur();
        lecteur.connecter();
        lecteur.MiseTension();
        lecteur.select();
        /*
        boolean test = lecteur.entrePin("1234");
        System.out.println(test);
        lecteur.entrePin("1234");*/

        boolean test2 = lecteur.entrePin("0000");
        //System.out.println(test2);
        boolean testchange = lecteur.changerPin("1234");
        //System.out.println(testchange);
        boolean test3 = lecteur.entrePin("1234");
        //System.out.println("apres changement: " + test3);
        System.out.println("interroger avant depot");
        System.out.println(lecteur.interroger());
        lecteur.depot(700);
        System.out.println("interroger apres depot");
        System.out.println(lecteur.interroger());
        lecteur.depot(200);
        System.out.println("interroger apres depot2");
        System.out.println(lecteur.interroger());

        lecteur.retrait(500);
        System.out.println("interroger apres retrait");
        System.out.println(lecteur.interroger());

        String[] his = lecteur.historique();
        int i = 0;
        while (his[i] != null) {
            System.out.println(his[i]);
            i++;
        }
        lecteur.HorsTension();
    }
}
