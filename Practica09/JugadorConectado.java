package practica09;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

public class JugadorConectado extends Thread {
    private Socket socket;
    private BufferedReader entrada;
    private PrintWriter salida;
    private String letra = "";
    private boolean turno = false;
    private JButton[] botones;
    private Jugador ventana;
    
    public JugadorConectado(Socket socket, JButton[] botones, Jugador ventana) {
        this.socket = socket;
        this.botones = botones;
        this.ventana = ventana;
        
        try {
            this.entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.salida = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error al inicializar conexión: " + e);
        }
    }
    
    @Override
    public void run() {
        try {
            String mensaje;
            while ((mensaje = entrada.readLine()) != null) {
                final String msg = mensaje;
                
                // Actualizar GUI en el hilo de eventos de Swing
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        procesarMensaje(msg);
                    }
                });
            }
        } catch (IOException e) {
            System.out.println("Error de conexión: " + e.getMessage());
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    ventana.mostrarError("Conexión perdida con el servidor");
                }
            });
        }
    }
    
    private void procesarMensaje(String mensaje) {
        String[] partes = mensaje.split(":");
        
        switch (partes[0]) {
            case "LETRA":
                this.letra = partes[1];
                ventana.setLetra(letra);
                System.out.println("Tu letra asignada: " + letra);
                break;
                
            case "INICIO":
                System.out.println("¡Juego iniciado!");
                break;
                
            case "TABLERO":
                if (partes.length > 1) {
                    String[] casillas = partes[1].split(",");
                    actualizarTablero(casillas);
                }
                break;
                
            case "TURNO":
                if (partes[1].equals("SI")) {
                    this.turno = true;
                    ventana.setEstado("Tu turno - Letra: " + letra);
                    habilitarBotones();
                } else {
                    this.turno = false;
                    ventana.setEstado("Esperando oponente - Letra: " + letra);
                    deshabilitarBotones();
                }
                break;
                
            case "GANADOR":
                String ganador = partes[1];
                if (ganador.equals(letra)) {
                    ventana.setEstado("¡GANASTE!");
                    ventana.mostrarMensaje("¡Felicidades! ¡Has ganado!");
                } else {
                    ventana.setEstado("Has perdido");
                    ventana.mostrarMensaje("Has perdido. ¡Mejor suerte la próxima!");
                }
                deshabilitarBotones();
                ventana.habilitarReiniciar();
                break;
                
            case "EMPATE":
                ventana.setEstado("¡Empate!");
                ventana.mostrarMensaje("¡Empate! Ambos jugaron muy bien.");
                deshabilitarBotones();
                ventana.habilitarReiniciar();
                break;
                
            case "REINICIO":
                limpiarTablero();
                ventana.setEstado("Juego reiniciado");
                ventana.deshabilitarReiniciar();
                break;
        }
    }
    
    public void enviarPosicion(int posicion) {
        if (turno && salida != null) {
            salida.println("MOVIMIENTO:" + posicion);
            System.out.println("Movimiento enviado: posición " + posicion);
        } else {
            System.out.println("No es tu turno o conexión no disponible");
        }
    }
    
    public void solicitarReinicio() {
        if (salida != null) {
            salida.println("REINICIAR");
            System.out.println("Solicitando reinicio del juego");
        }
    }
    
    private void actualizarTablero(String[] casillas) {
        for (int i = 0; i < Math.min(9, casillas.length); i++) {
            String texto = casillas[i].equals("-") ? "" : casillas[i];
            botones[i].setText(texto);
            
            // Cambiar color según la letra
            if (!texto.isEmpty()) {
                if (texto.equals("X")) {
                    botones[i].setForeground(java.awt.Color.BLUE);
                } else {
                    botones[i].setForeground(java.awt.Color.RED);
                }
                botones[i].setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 36));
            }
        }
    }
    
    private void limpiarTablero() {
        for (JButton boton : botones) {
            boton.setText("");
            boton.setEnabled(true);
        }
    }
    
    private void habilitarBotones() {
        for (int i = 0; i < botones.length; i++) {
            // Solo habilitar botones vacíos
            if (botones[i].getText().isEmpty()) {
                botones[i].setEnabled(true);
            }
        }
    }
    
    private void deshabilitarBotones() {
        for (JButton boton : botones) {
            boton.setEnabled(false);
        }
    }
    
    public String getLetra() {
        return letra;
    }
    
    public boolean getTurno() {
        return turno;
    }
    
    public void cerrarConexion() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error al cerrar conexión: " + e.getMessage());
        }
    }
}
