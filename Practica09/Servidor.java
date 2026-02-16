
package practica09;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Servidor {
    static String[] tablaJuego = new String[9];
    static int PUERTO = 5000;
    static ArrayList<ManejadorJugador> jugadores = new ArrayList<ManejadorJugador>();
    static boolean jugando = false;
    static int turnoActual = 0;
    
    public static void main(String[] args) {
        Servidor servidor = new Servidor();
        servidor.iniciar();
    }
    
    public void iniciar() {
        inicializarJuego();
        
        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("=================================");
            System.out.println("Servidor iniciado en puerto " + PUERTO);
            System.out.println("Esperando jugadores...");
            System.out.println("=================================\n");
            
            // Esperar a que se conecten 2 jugadores
            while (jugadores.size() < 2) {
                Socket socket = serverSocket.accept();
                String letra = jugadores.isEmpty() ? "X" : "O";
                ManejadorJugador manejador = new ManejadorJugador(socket, letra, jugadores.size());
                jugadores.add(manejador);
                new Thread(manejador).start();
                
                System.out.println("Jugador " + (jugadores.size()) + " conectado (" + letra + ")");
            }
            
            System.out.println("\n¡Ambos jugadores conectados!");
            System.out.println("Iniciando juego...\n");
            
            // Enviar información inicial a ambos jugadores
            enviarATodos("INICIO");
            enviarTableroATodos();
            
            // Comenzar juego
            jugando = true;
            turnoActual = (int) (Math.random() * 2);
            System.out.println("Comienza el jugador " + jugadores.get(turnoActual).letra + "\n");
            notificarTurno();
            
            // Mantener servidor activo
            while (jugando) {
                Thread.sleep(100);
            }
            
            System.out.println("\nJuego terminado. Esperando nuevas conexiones...");
            
        } catch (IOException | InterruptedException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static synchronized void procesarMovimiento(int posicion, String letra, int indiceJugador) {
        if (!jugando) return;
        
        // Verificar que sea el turno correcto
        if (indiceJugador != turnoActual) {
            System.out.println("No es el turno del jugador " + letra);
            return;
        }
        
        // Verificar que la posición sea válida
        if (posicion < 0 || posicion >= 9 || !tablaJuego[posicion].equals("-")) {
            System.out.println("Movimiento inválido en posición " + posicion);
            return;
        }
        
        // Realizar movimiento
        tablaJuego[posicion] = letra;
        System.out.println("Jugador " + letra + " colocó en posición " + posicion);
        
        // Enviar tablero actualizado
        enviarTableroATodos();
        mostrarTablero();
        
        // Verificar ganador
        if (verificarGanador(letra)) {
            System.out.println("\n¡GANADOR: " + letra + "!\n");
            enviarATodos("GANADOR:" + letra);
            jugando = false;
            return;
        }
        
        // Verificar empate
        if (verificarEmpate()) {
            System.out.println("\n¡EMPATE!\n");
            enviarATodos("EMPATE");
            jugando = false;
            return;
        }
        
        // Cambiar turno
        turnoActual = (turnoActual == 0) ? 1 : 0;
        notificarTurno();
    }
    
    private static void notificarTurno() {
        for (int i = 0; i < jugadores.size(); i++) {
            ManejadorJugador jugador = jugadores.get(i);
            if (i == turnoActual) {
                jugador.enviar("TURNO:SI");
                System.out.println("Turno de " + jugador.letra);
            } else {
                jugador.enviar("TURNO:NO");
            }
        }
    }
    
    private static void enviarTableroATodos() {
        StringBuilder mensaje = new StringBuilder("TABLERO:");
        for (String casilla : tablaJuego) {
            mensaje.append(casilla).append(",");
        }
        enviarATodos(mensaje.toString());
    }
    
    private static void enviarATodos(String mensaje) {
        for (ManejadorJugador jugador : jugadores) {
            jugador.enviar(mensaje);
        }
    }
    
    private void inicializarJuego() {
        for (int i = 0; i < tablaJuego.length; i++) {
            tablaJuego[i] = "-";
        }
    }
    
    private static void mostrarTablero() {
        System.out.println("\n--- TABLERO ACTUAL ---");
        System.out.println("  " + tablaJuego[0] + " | " + tablaJuego[1] + " | " + tablaJuego[2]);
        System.out.println(" -----------");
        System.out.println("  " + tablaJuego[3] + " | " + tablaJuego[4] + " | " + tablaJuego[5]);
        System.out.println(" -----------");
        System.out.println("  " + tablaJuego[6] + " | " + tablaJuego[7] + " | " + tablaJuego[8]);
        System.out.println("----------------------\n");
    }
    
    private static boolean verificarGanador(String letra) {
        int[][] combinaciones = {
            {0, 1, 2}, {3, 4, 5}, {6, 7, 8}, // Horizontales
            {0, 3, 6}, {1, 4, 7}, {2, 5, 8}, // Verticales
            {0, 4, 8}, {2, 4, 6}              // Diagonales
        };
        
        for (int[] combo : combinaciones) {
            if (tablaJuego[combo[0]].equals(letra) &&
                tablaJuego[combo[1]].equals(letra) &&
                tablaJuego[combo[2]].equals(letra)) {
                return true;
            }
        }
        return false;
    }
    
    private static boolean verificarEmpate() {
        for (String casilla : tablaJuego) {
            if (casilla.equals("-")) {
                return false;
            }
        }
        return true;
    }
    
    // Clase interna para manejar cada jugador
    static class ManejadorJugador implements Runnable {
        private Socket socket;
        private BufferedReader entrada;
        private PrintWriter salida;
        private String letra;
        private int indice;
        
        public ManejadorJugador(Socket socket, String letra, int indice) {
            this.socket = socket;
            this.letra = letra;
            this.indice = indice;
            
            try {
                entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                salida = new PrintWriter(socket.getOutputStream(), true);
                
                // Enviar letra asignada
                salida.println("LETRA:" + letra);
                
            } catch (IOException e) {
                System.out.println("Error al inicializar jugador: " + e.getMessage());
            }
        }
        
        @Override
        public void run() {
            try {
                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    if (mensaje.startsWith("MOVIMIENTO:")) {
                        int posicion = Integer.parseInt(mensaje.split(":")[1]);
                        procesarMovimiento(posicion, letra, indice);
                    } else if (mensaje.equals("REINICIAR")) {
                        reiniciarJuego();
                    }
                }
            } catch (IOException e) {
                System.out.println("Jugador " + letra + " desconectado");
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                jugadores.remove(this);
            }
        }
        
        public void enviar(String mensaje) {
            if (salida != null) {
                salida.println(mensaje);
            }
        }
    } 
    
    private static void reiniciarJuego() {
        for (int i = 0; i < tablaJuego.length; i++) {
            tablaJuego[i] = "-";
        }
        jugando = true;
        turnoActual = (int) (Math.random() * 2);
        enviarATodos("REINICIO");
        enviarTableroATodos();
        notificarTurno();
        System.out.println("\n=== JUEGO REINICIADO ===\n");
    }
}
