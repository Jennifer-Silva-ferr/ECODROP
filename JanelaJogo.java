import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class JanelaJogo extends JFrame {

    // =========================================================================
    // 🎨 CONFIGURAÇÕES GRÁFICAS E DE INTERFACE 🎨
    // =========================================================================

    private String URL_FUNDO = "Imagens/fundo.png";

    private static String IMG_PLASTICO = "Imagens/lixoplastico.png";
    private static String IMG_PAPEL = "Imagens/lixopapel.png";
    private static String IMG_VIDRO = "Imagens/lixovidro.png";
    private static String IMG_ORGANICO = "Imagens/lixoorganico.png";
    private static String IMG_TOXICO = "Imagens/lixotoxico.png";
    private static String IMG_DOURADO = "Imagens/lixodourado.png";
    private static String IMG_BATERIA = "Imagens/lixometal.png";
    private static String IMG_MISTERIO = "Imagens/lixomisterio.png";

    private String SOM_ABRIR = "audios/abrir.wav";
    private String SOM_FECHAR = "audios/fechar.wav";
    private String SOM_PONTUAR = "audios/pontuar.wav";
    private String SOM_ERRO = "audios/erro.wav";
    private String MUSICA_FUNDO = "audios/musica.wav";

    // Variáveis Globais de Áudio
    public static boolean isMudo = false;
    public static Clip clipMusicaFundo;

    // =========================================================================
    // ⚙️ LÓGICA DO JOGO ⚙️
    // =========================================================================

    private CardLayout cards = new CardLayout();
    private JPanel painelPrincipal = new JPanel(cards);

    private int pontuacao = 0, poluicao = 0, nivel = 1;
    private double gravidadeBase = 3.0;
    private int velocidadeGeracao = 1500;
    private int lixosAcertadosSeguidos = 0;
    private int multiplicadorCombo = 1;

    private double multVelocidade = 1.0;
    private double multPreco = 1.0;

    private boolean modoCameraLenta = false, modoFrenesi = false;
    private int forcaVentoX = 0;
    private int frameVento = 0;

    private JLabel labelPontuacao, labelPoluicao, labelNivel, labelAlerta;
    private JButton btnSomTopo;
    private JPanel painelJogo;
    private Map<TipoLixo, JLabel> lixeiras = new HashMap<>();
    private Random random = new Random();
    private JLabel lixoSendoArrastado = null;
    private Image imagemDeFundo;

    private Timer geradorLixo, motorGravidade, motorEventosClimaticos;

    public JanelaJogo() {
        setTitle("EcoDrop V3: Ultimate Edition");
        setSize(850, 700); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 

        JPanel painelMenu = criarPainelMenu();
        JPanel painelContainerJogo = criarPainelJogoCompleto();

        painelPrincipal.add(painelMenu, "MENU");
        painelPrincipal.add(painelContainerJogo, "JOGO");

        add(painelPrincipal);
        cards.show(painelPrincipal, "MENU"); 
        
        tocarMusicaFundo(MUSICA_FUNDO);
    }

    private JPanel criarPainelMenu() {
        JPanel menu = new JPanel();
        menu.setLayout(null);
        menu.setBackground(new Color(35, 35, 35));

        JLabel titulo = new JLabel("ECODROP", SwingConstants.CENTER);
        titulo.setForeground(Color.WHITE);
        titulo.setFont(new Font("Arial", Font.BOLD, 46)); // Título maior
        titulo.setBounds(-20, 100, 850, 60);

        JButton btnFacil = new JButton("FÁCIL");
        btnFacil.setBounds(250, 200, 300, 70);

        JButton btnMedio = new JButton("MÉDIO");
        btnMedio.setBounds(250, 300, 300, 70);

        JButton btnDificil = new JButton("DIFÍCIL");
        btnDificil.setBounds(250, 400, 300, 70);

        estilizarBotao(btnFacil, new Color(76, 175, 80));
        estilizarBotao(btnMedio, new Color(255, 193, 7));
        estilizarBotao(btnDificil, new Color(244, 67, 54));

        btnFacil.addActionListener(e -> iniciarJogo(0.5, 0.5));
        btnMedio.addActionListener(e -> iniciarJogo(1.0, 1.0));
        btnDificil.addActionListener(e -> iniciarJogo(1.5, 1.5));

        // --- ADIÇÃO DOS CRÉDITOS ---
        JLabel creditos = new JLabel("Equipe Dev: Marcus - Geovana - Jennifer - Matheus", SwingConstants.CENTER);
        creditos.setForeground(new Color(160, 160, 160)); // Cinza claro
        creditos.setFont(new Font("Arial", Font.BOLD | Font.ITALIC, 16));
        creditos.setBounds(0, 600, 850, 30);

        menu.add(titulo);
        menu.add(btnFacil);
        menu.add(btnMedio);
        menu.add(btnDificil);
        menu.add(creditos); // Adicionando os créditos na tela

        return menu;
    }

    private void estilizarBotao(JButton botao, Color cor) {
        botao.setBackground(cor);
        botao.setForeground(Color.WHITE);
        botao.setFont(new Font("Arial", Font.BOLD, 24));
        botao.setFocusPainted(false);
        botao.setBorderPainted(false);
        botao.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private void iniciarJogo(double vel, double preco) {
        this.multVelocidade = vel;
        this.multPreco = preco;
        this.gravidadeBase = 3.0 * multVelocidade; 

        cards.show(painelPrincipal, "JOGO"); 
        iniciarMotores();
        atualizarHUD();
    }

    // =========================================================================
    // MÉTODO ESPECIAL PARA CRIAR TEXTOS COM SOMBRA (HUD)
    // =========================================================================
    private JLabel criarShadowLabel(String texto, int x, int y, int w, int h, int alinhamento, int tamanhoFonte) {
        JLabel label = new JLabel(texto, alinhamento) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                
                String txt = getText();
                FontMetrics fm = g2.getFontMetrics(getFont());
                
                // Calcula alinhamento
                int textX = 0;
                if (getHorizontalAlignment() == SwingConstants.CENTER) {
                    textX = (getWidth() - fm.stringWidth(txt)) / 2;
                } else if (getHorizontalAlignment() == SwingConstants.RIGHT) {
                    textX = getWidth() - fm.stringWidth(txt);
                }
                int textY = fm.getAscent() + (getHeight() - fm.getHeight()) / 2;

                // 1. Desenha a sombra (preta, translúcida, levemente deslocada)
                g2.setColor(new Color(0, 0, 0, 180)); 
                g2.drawString(txt, textX + 2, textY + 2); // Deslocamento de 2 pixels

                // 2. Desenha o texto principal por cima
                g2.setColor(getForeground());
                g2.drawString(txt, textX, textY);
                g2.dispose();
            }
        };
        label.setBounds(x, y, w, h);
        label.setFont(new Font("Arial", Font.BOLD, tamanhoFonte));
        return label;
    }

    private JPanel criarPainelJogoCompleto() {
        JPanel container = new JPanel(new BorderLayout());
        carregarFundo();

        painelJogo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (imagemDeFundo != null) g.drawImage(imagemDeFundo, 0, 0, getWidth(), getHeight(), this);
                desenharEfeitoVento((Graphics2D) g); 
            }
        };
        painelJogo.setLayout(null); 

        // Usando as novas Labels com sombra para destacar do fundo
        labelPontuacao = criarShadowLabel("🌟 Pontos: 0", 10, 10, 300, 30, SwingConstants.LEFT, 18);
        labelPontuacao.setForeground(Color.WHITE); // Branco para destacar na sombra

        labelNivel = criarShadowLabel("Nível: 1", 340, 10, 100, 30, SwingConstants.LEFT, 18);
        labelNivel.setForeground(new Color(135, 206, 250)); // Azul claro

        labelPoluicao = criarShadowLabel("⚠️ Poluição: 0%", 500, 10, 180, 30, SwingConstants.LEFT, 18);
        labelPoluicao.setForeground(new Color(255, 100, 100)); // Vermelho claro/salmão

        labelAlerta = criarShadowLabel("", 0, 150, 850, 50, SwingConstants.CENTER, 36);

        // Botão de Áudio
        btnSomTopo = new JButton("🔊 Áudio");
        btnSomTopo.setBounds(710, 10, 100, 30);
        btnSomTopo.setBackground(new Color(60, 60, 60));
        btnSomTopo.setForeground(Color.WHITE);
        btnSomTopo.setFocusPainted(false);
        btnSomTopo.addActionListener(e -> alternarAudio());

        painelJogo.add(labelPontuacao); painelJogo.add(labelNivel);
        painelJogo.add(labelPoluicao); painelJogo.add(labelAlerta);
        painelJogo.add(btnSomTopo);

        criarLixeira(TipoLixo.PAPEL, "Papel", "Imagens/papel.png", "Imagens/papel_aberto.jpg", 5);
        criarLixeira(TipoLixo.PLASTICO, "Plástico", "Imagens/plastico.png", "Imagens/plastico_aberto.jpg", 120);
        criarLixeira(TipoLixo.VIDRO, "Vidro", "Imagens/vidro.png", "Imagens/vidro_aberto.jpg", 235);
        criarLixeira(TipoLixo.ORGANICO, "Orgânico", "Imagens/organico.png", "Imagens/organico_aberto.jpg", 350);
        criarLixeira(TipoLixo.METAL, "Metal", "Imagens/metal.png", "Imagens/metal_aberto.jpg", 465);
        criarLixeira(TipoLixo.TOXICO, "Perigoso", "Imagens/toxico.png", "Imagens/toxico_aberto.jpg", 580);
        criarLixeira(TipoLixo.DOURADO, "Relíquia", "Imagens/dourado.png", "Imagens/dourado_aberto.jpg", 695);

        container.add(painelJogo, BorderLayout.CENTER);

        JPanel painelLoja = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        painelLoja.setBackground(Color.DARK_GRAY);

        int precoFiltro = (int)(100 * multPreco);
        int precoCamera = (int)(150 * multPreco);

        JButton btnFiltro = new JButton("Filtro (-20%) - " + precoFiltro + " 🌟");
        JButton btnCameraLenta = new JButton("Câmera Lenta - " + precoCamera + " 🌟");

        btnFiltro.addActionListener(e -> {
            if (pontuacao >= precoFiltro) {
                pontuacao -= precoFiltro; poluicao = Math.max(0, poluicao - 20);
                mostrarAlertaGrande("Filtro Ativado!", Color.CYAN); atualizarHUD();
            }
        });

        btnCameraLenta.addActionListener(e -> {
            if (pontuacao >= precoCamera) {
                pontuacao -= precoCamera; modoCameraLenta = true;
                mostrarAlertaGrande("Câmera Lenta!", Color.MAGENTA);
                new Timer(10000, ev -> modoCameraLenta = false).start(); 
                atualizarHUD();
            }
        });

        painelLoja.add(btnFiltro); painelLoja.add(btnCameraLenta);
        container.add(painelLoja, BorderLayout.SOUTH);

        return container;
    }

    // =========================================================================
    // SISTEMAS DE ÁUDIO E VISUAL EFEITOS
    // =========================================================================
    
    private void alternarAudio() {
        isMudo = !isMudo;
        if (isMudo) {
            btnSomTopo.setText("🔇 Mutado");
            if (clipMusicaFundo != null) clipMusicaFundo.stop();
        } else {
            btnSomTopo.setText("🔊 Áudio");
            if (clipMusicaFundo != null) clipMusicaFundo.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    private void tocarSom(String caminho) {
        if (isMudo) return; 
        new Thread(() -> {
            try {
                File arquivoSom = obterArquivoLocal(caminho);
                if (arquivoSom.exists()) {
                    AudioInputStream audioInput = AudioSystem.getAudioInputStream(arquivoSom);
                    Clip clip = AudioSystem.getClip();
                    clip.open(audioInput);
                    clip.start();
                }
            } catch (Exception e) {}
        }).start();
    }

    private void tocarMusicaFundo(String caminho) {
        new Thread(() -> {
            try {
                File arquivoMusica = obterArquivoLocal(caminho);
                if (arquivoMusica.exists()) {
                    AudioInputStream audioInput = AudioSystem.getAudioInputStream(arquivoMusica);
                    clipMusicaFundo = AudioSystem.getClip();
                    clipMusicaFundo.open(audioInput);
                    if (!isMudo) clipMusicaFundo.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } catch (Exception e) {}
        }).start();
    }

    private void desenharEfeitoVento(Graphics2D g2) {
        if (forcaVentoX == 0) return;
        frameVento += forcaVentoX; 

        g2.setColor(new Color(255, 255, 255, 100)); 
        g2.setStroke(new BasicStroke(3));
        
        for (int i = 0; i < 8; i++) {
            int y = 50 + (i * 50);
            int x = (frameVento + (i * 120)) % 1000;
            if (x < -100) x += 1000; 
            g2.drawLine(x, y, x + (forcaVentoX > 0 ? 60 : -60), y);
        }
    }

    private File obterArquivoLocal(String caminho) {
        File arquivo = new File(caminho);
        if (arquivo.exists()) return arquivo;
        File arquivoNaPasta01 = new File("01/" + caminho);
        if (arquivoNaPasta01.exists()) return arquivoNaPasta01;
        try {
            String basePath = JanelaJogo.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
            File projectDir = new File(basePath).getParentFile();
            File localFile = new File(projectDir, caminho);
            if (localFile.exists()) return localFile;
        } catch (Exception e) {}
        return new File(caminho);
    }

    private void carregarFundo() {
        try { imagemDeFundo = ImageIO.read(obterArquivoLocal(URL_FUNDO)); } catch (Exception e) {}
    }

    private void criarLixeira(TipoLixo tipo, String nome, String caminhoFechada, String caminhoAberta, int posX) {
        JLabel lixeira = new JLabel();
        lixeira.setBounds(posX, 420, 110, 80);
        lixeira.setOpaque(true);
        lixeira.setBackground(new Color(200, 200, 200)); 
        lixeira.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        ImageIcon iconFechada = null, iconAberta = null;
        try {
            Image imgF = ImageIO.read(obterArquivoLocal(caminhoFechada));
            iconFechada = new ImageIcon(imgF.getScaledInstance(110, 80, Image.SCALE_SMOOTH));
            Image imgA = ImageIO.read(obterArquivoLocal(caminhoAberta));
            iconAberta = new ImageIcon(imgA.getScaledInstance(110, 80, Image.SCALE_SMOOTH));
        } catch (Exception e) {}

        if (iconFechada != null) lixeira.setIcon(iconFechada);
        else { lixeira.setText(nome); lixeira.setHorizontalAlignment(SwingConstants.CENTER); }

        lixeira.putClientProperty("iconFechada", iconFechada);
        lixeira.putClientProperty("iconAberta", iconAberta);
        lixeira.putClientProperty("estadoAberta", false);

        painelJogo.add(lixeira);
        lixeiras.put(tipo, lixeira);
    }

    private void iniciarMotores() {
        geradorLixo = new Timer(velocidadeGeracao, e -> gerarLixo());
        geradorLixo.start();

        motorGravidade = new Timer(40, e -> aplicarGravidade());
        motorGravidade.start();

        motorEventosClimaticos = new Timer(8000, e -> aplicarClimaAleatorio());
        motorEventosClimaticos.start();
    }

    private void aplicarClimaAleatorio() {
        int chance = random.nextInt(100);
        if (chance < 25) { 
            forcaVentoX = random.nextBoolean() ? 5 : -5;
            mostrarAlertaGrande("🌪️ VENTO FORTE!", Color.LIGHT_GRAY);
            new Timer(4000, ev -> forcaVentoX = 0).start(); 
        } else if (chance < 40) {
            mostrarAlertaGrande("🌧️ CHUVA DE LIXO!", Color.BLUE);
            for(int i=0; i<4; i++) gerarLixo();
        }
    }

    private void gerarLixo() {
        Residuo logico; int sorteio = random.nextInt(100);

        if (sorteio < 5) logico = new LixoDourado(IMG_DOURADO);
        else if (sorteio < 15) logico = new LixoToxico(IMG_TOXICO);
        else if (sorteio < 25) logico = new Bateria(IMG_BATERIA);
        else if (sorteio < 45) logico = new GarrafaPlastica(IMG_PLASTICO);
        else if (sorteio < 65) logico = new CaixaPapelao(IMG_PAPEL);
        else if (sorteio < 85) logico = new GarrafaVidro(IMG_VIDRO);
        else logico = new CascaDeBanana(IMG_ORGANICO);

        JLabel visual = new JLabel();
        visual.putClientProperty("logica", logico);

        boolean isMisterio = random.nextInt(100) < 15;
        visual.putClientProperty("misterio", isMisterio);
        configurarVisual(visual, logico, isMisterio);

        visual.setLocation(random.nextInt(Math.max(1, painelJogo.getWidth() - 60)), 0);
        Point offset = new Point();

        visual.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                offset.x = e.getX(); offset.y = e.getY();
                painelJogo.setComponentZOrder(visual, 0);
                lixoSendoArrastado = visual;

                if ((Boolean) visual.getClientProperty("misterio")) {
                    visual.putClientProperty("misterio", false);
                    configurarVisual(visual, logico, false);
                }
            }
            @Override public void mouseReleased(MouseEvent e) {
                lixoSendoArrastado = null; 
                verificarColisao(visual, logico);
                
                for (JLabel lixeira : lixeiras.values()) {
                    lixeira.putClientProperty("estadoAberta", false);
                    Icon fechada = (Icon) lixeira.getClientProperty("iconFechada");
                    if (fechada != null) lixeira.setIcon(fechada);
                }
            }
        });

        visual.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                visual.setLocation(visual.getX() + e.getX() - offset.x, visual.getY() + e.getY() - offset.y);
                
                Rectangle boundsLixo = visual.getBounds();
                boundsLixo.grow(10, 10); 
                
                for (JLabel lixeira : lixeiras.values()) {
                    boolean intersecta = boundsLixo.intersects(lixeira.getBounds());
                    boolean estadoAberto = (Boolean) lixeira.getClientProperty("estadoAberta");

                    if (intersecta && !estadoAberto) {
                        lixeira.putClientProperty("estadoAberta", true);
                        Icon aberta = (Icon) lixeira.getClientProperty("iconAberta");
                        if (aberta != null) lixeira.setIcon(aberta);
                        tocarSom(SOM_ABRIR);
                    } else if (!intersecta && estadoAberto) {
                        lixeira.putClientProperty("estadoAberta", false);
                        Icon fechada = (Icon) lixeira.getClientProperty("iconFechada");
                        if (fechada != null) lixeira.setIcon(fechada);
                        tocarSom(SOM_FECHAR);
                    }
                }
            }
        });
        painelJogo.add(visual, 0);
    }

    private void configurarVisual(JLabel label, Residuo res, boolean oculto) {
        String caminho = oculto ? IMG_MISTERIO : res.getUrlImagem();
        try {
            Image img = ImageIO.read(obterArquivoLocal(caminho))
                    .getScaledInstance(60, 60, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(img));
            label.setText("");
            label.setSize(60, 60);
        } catch (Exception e) {
            label.setText(oculto ? "❓" : res.getNome());
            label.setSize(90, 40);
            label.setOpaque(true);
            label.setBackground(Color.WHITE);
        }
    }

    private void aplicarGravidade() {
        for (Component comp : painelJogo.getComponents()) {
            if (comp instanceof JLabel && !lixeiras.containsValue(comp) && comp.getY() < 500 && comp != labelPontuacao && comp != labelPoluicao && comp != labelNivel && comp != labelAlerta && comp != btnSomTopo) {
                JLabel lixo = (JLabel) comp;

                if (lixo != lixoSendoArrastado && lixo.getClientProperty("logica") != null) {
                    Residuo res = (Residuo) lixo.getClientProperty("logica");

                    int queda = (modoCameraLenta || modoFrenesi) ? 1 : (int)(gravidadeBase * res.getMultiplicadorGravidade());
                    lixo.setLocation(lixo.getX() + forcaVentoX, lixo.getY() + queda);

                    if (lixo.getY() > 430) {
                        painelJogo.remove(lixo);
                        if (!modoFrenesi) {
                            poluicao += res.getImpactoPoluicao();
                            lixosAcertadosSeguidos = 0;
                            multiplicadorCombo = 1;
                            tocarSom(SOM_ERRO);
                        }
                        atualizarHUD(); verificarGameOver();
                    }
                }
            }
        }
        painelJogo.repaint();
    }

    private void verificarColisao(JLabel visual, Residuo res) {
        Rectangle areaLixo = visual.getBounds();
        areaLixo.grow(20, 20); 

        for (TipoLixo tipo : lixeiras.keySet()) {
            if (areaLixo.intersects(lixeiras.get(tipo).getBounds())) {

                if (tipo == res.getTipo()) {
                    if (res instanceof LixoDourado) {
                        poluicao = 0; mostrarAlertaGrande("LIMPEZA TOTAL!", Color.YELLOW);
                        tocarSom(SOM_PONTUAR);
                    } else {
                        lixosAcertadosSeguidos++;
                        
                        if (lixosAcertadosSeguidos >= 3) multiplicadorCombo = 2;
                        if (lixosAcertadosSeguidos >= 6) multiplicadorCombo = 3;
                        if (lixosAcertadosSeguidos >= 10) {
                            multiplicadorCombo = 4;
                            poluicao = 0; 
                            mostrarAlertaGrande("✨ COMBO MÁXIMO! POLUIÇÃO ZERADA!", Color.YELLOW);
                        }

                        int ganhos = res.getPoints() * multiplicadorCombo;

                        if (visual.getY() < 200) {
                            ganhos += 10;
                            mostrarTextoFlutuante("BÔNUS ALTURA!", Color.CYAN, visual.getX(), visual.getY() - 20);
                        }
                        
                        pontuacao += ganhos;
                        tocarSom(SOM_PONTUAR);
                        mostrarTextoFlutuante("+" + ganhos, Color.GREEN, visual.getX(), visual.getY());
                    }
                    painelJogo.remove(visual); verificarSubidaDeNivel();

                } else {
                    poluicao += res.getImpactoPoluicao(); 
                    lixosAcertadosSeguidos = 0; 
                    multiplicadorCombo = 1;
                    tocarSom(SOM_ERRO);
                    mostrarTextoFlutuante("ERROU", Color.RED, visual.getX(), visual.getY());
                    painelJogo.remove(visual);
                }
                break;
            }
        }
        atualizarHUD();
    }

    private void mostrarTextoFlutuante(String texto, Color cor, int x, int y) {
        // Label com sombra.
        JLabel flutuante = criarShadowLabel(texto, x, y, 200, 30, SwingConstants.LEFT, 18);
        flutuante.setForeground(cor);
        painelJogo.add(flutuante, 0);
        new Timer(50, new ActionListener() {
            int ticks = 0;
            @Override public void actionPerformed(ActionEvent e) {
                flutuante.setLocation(flutuante.getX(), flutuante.getY() - 3); 
                if(++ticks > 15) { painelJogo.remove(flutuante); ((Timer)e.getSource()).stop(); }
            }
        }).start();
    }

    private void verificarSubidaDeNivel() {
        int novoNivel = (pontuacao / 100) + 1;
        if (novoNivel > nivel) {
            nivel = novoNivel;
            gravidadeBase += 0.5 * multVelocidade; 

            velocidadeGeracao = Math.max(400, velocidadeGeracao - 80);
            geradorLixo.setDelay((int)(velocidadeGeracao / multVelocidade));

            if (nivel % 5 == 0) {
                modoFrenesi = true; mostrarAlertaGrande("🔥 FRENESI! (10s)", Color.YELLOW);
                new Timer(10000, e -> modoFrenesi = false).start();
            } else {
                mostrarAlertaGrande("NÍVEL " + nivel, Color.BLUE);
            }
        }
    }

    private void atualizarHUD() {
        labelPontuacao.setText("🌟 Pontos: " + pontuacao);
        labelNivel.setText("Nível: " + nivel);
        labelPoluicao.setText("⚠️ Poluição: " + poluicao + "%");
        
        if (multiplicadorCombo > 1) {
            labelPontuacao.setForeground(Color.ORANGE);
            labelPontuacao.setText("🌟 Pontos: " + pontuacao + " (" + multiplicadorCombo + "x)");
        } else {
            labelPontuacao.setForeground(Color.WHITE); // Volta para branco (contrasta com a sombra)
        }
    }

    private void mostrarAlertaGrande(String msg, Color cor) {
        labelAlerta.setText(msg); labelAlerta.setForeground(cor);
        new Timer(2000, e -> labelAlerta.setText("")).start();
    }

    private void verificarGameOver() {
        if (poluicao >= 100) {
            geradorLixo.stop(); motorGravidade.stop();
            JOptionPane.showMessageDialog(this, "Game Over!\nVocê chegou ao Nível " + nivel + "\nPontos: " + pontuacao);
            System.exit(0); 
        }
    }
}

// =========================================================================
// CLASSES DE LÓGICA
// =========================================================================

enum TipoLixo { PLASTICO, PAPEL, ORGANICO, VIDRO, TOXICO, DOURADO, METAL }

abstract class Residuo {
    protected String nome; protected int points; protected int impactoPoluicao; protected TipoLixo tipo;
    protected String urlImagem; protected int multiplicadorGravidade = 1;

    public Residuo(String n, int p, int i, TipoLixo t, String url) {
        nome = n; points = p; impactoPoluicao = i; tipo = t; urlImagem = url;
    }
    public String getNome() { return nome; }
    public int getPoints() { return points; }
    public int getImpactoPoluicao() { return impactoPoluicao; }
    public TipoLixo getTipo() { return tipo; }
    public String getUrlImagem() { return urlImagem; }
    public int getMultiplicadorGravidade() { return multiplicadorGravidade; }
}

class GarrafaPlastica extends Residuo { public GarrafaPlastica(String img) { super("Plástico", 15, 20, TipoLixo.PLASTICO, img); } }
class CaixaPapelao extends Residuo { public CaixaPapelao(String img) { super("Papelão", 10, 10, TipoLixo.PAPEL, img); } }
class GarrafaVidro extends Residuo { public GarrafaVidro(String img) { super("Vidro", 20, 15, TipoLixo.VIDRO, img); } }
class CascaDeBanana extends Residuo { public CascaDeBanana(String img) { super("Organico", 10, 10, TipoLixo.ORGANICO, img); } }
class LixoDourado extends Residuo { public LixoDourado(String img) { super("Dourado", 0, 0, TipoLixo.DOURADO, img); } }
class LixoToxico extends Residuo { public LixoToxico(String img) { super("Tóxico", 30, 30, TipoLixo.TOXICO, img); } }
class Bateria extends Residuo {
    public Bateria(String img) {
        super("Bateria", 25, 20, TipoLixo.METAL, img);
        this.multiplicadorGravidade = 2;
    }
}