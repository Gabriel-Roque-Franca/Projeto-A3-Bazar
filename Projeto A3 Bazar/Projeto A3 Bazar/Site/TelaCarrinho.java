// TelaCarrinho.java
package Site;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.sql.*;
import java.util.List;

public class TelaCarrinho extends JFrame {

    private JList<ItemCarrinho> listaItens;
    private DefaultListModel<ItemCarrinho> listModel;
    private JLabel totalLabel;
    private JTextArea enderecoTextArea;
    private JRadioButton pixRadioButton;
    private JRadioButton creditoRadioButton;
    private JButton confirmarCompraButton;

    private int usuarioIdCliente = SessaoUsuario.getInstance().getUsuarioId() != null ?
            SessaoUsuario.getInstance().getUsuarioId() : 1;

    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/banco_bazar";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Gabriel_971";

    public TelaCarrinho() {
        setTitle("Carrinho de Compras");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        inicializarComponentes();
        inicializarEventos();

        atualizarListaItens();
        atualizarTotalLabel();

        setVisible(true);
    }

    private void inicializarComponentes() {
        listModel = new DefaultListModel<>();
        listaItens = new JList<>(listModel);
        listaItens.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel listaPanel = new JPanel(new BorderLayout());
        listaPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        listaPanel.add(new JScrollPane(listaItens), BorderLayout.CENTER);

        JButton removerItemButton = new JButton("Remover Item");
        JButton alterarQtdButton = new JButton("Alterar Qtd.");

        JPanel botoesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoesPanel.add(removerItemButton);
        botoesPanel.add(alterarQtdButton);
        listaPanel.add(botoesPanel, BorderLayout.SOUTH);

        JPanel checkoutPanel = new JPanel();
        checkoutPanel.setLayout(new BoxLayout(checkoutPanel, BoxLayout.Y_AXIS));
        checkoutPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        totalLabel = new JLabel("Total: R$ 0.00");
        totalLabel.setFont(new Font("Arial", Font.BOLD, 16));

        enderecoTextArea = new JTextArea(5, 20);
        enderecoTextArea.setBorder(BorderFactory.createTitledBorder("Endereço de Entrega:"));

        JPanel pagamentoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pagamentoPanel.setBorder(BorderFactory.createTitledBorder("Pagamento"));
        pixRadioButton = new JRadioButton("Pix");
        creditoRadioButton = new JRadioButton("Cartão de Crédito");
        ButtonGroup pagamentoGroup = new ButtonGroup();
        pagamentoGroup.add(pixRadioButton);
        pagamentoGroup.add(creditoRadioButton);
        pixRadioButton.setSelected(true);
        pagamentoPanel.add(pixRadioButton);
        pagamentoPanel.add(creditoRadioButton);

        confirmarCompraButton = new JButton("Confirmar Compra");

        checkoutPanel.add(totalLabel);
        checkoutPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        checkoutPanel.add(new JScrollPane(enderecoTextArea));
        checkoutPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        checkoutPanel.add(pagamentoPanel);
        checkoutPanel.add(Box.createVerticalGlue());
        checkoutPanel.add(confirmarCompraButton);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.add(checkoutPanel, BorderLayout.NORTH);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, listaPanel, rightPanel);
        splitPane.setDividerLocation(400);
        add(splitPane, BorderLayout.CENTER);

        // Eventos dos botões de remover e alterar
        removerItemButton.addActionListener(e -> removerItemSelecionado());
        alterarQtdButton.addActionListener(e -> alterarQuantidadeSelecionada());
    }

    private void inicializarEventos() {
        confirmarCompraButton.addActionListener(e -> processarCompra());
    }

    private void removerItemSelecionado() {
        int selectedIndex = listaItens.getSelectedIndex();
        if (selectedIndex != -1) {
            ItemCarrinho item = listModel.getElementAt(selectedIndex);
            Carrinho.getInstance().removerItem(item.getProdutoId(), item.getTamanho());
            atualizarListaItens();
            atualizarTotalLabel();
        } else {
            JOptionPane.showMessageDialog(this, "Selecione um item para remover.", "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void alterarQuantidadeSelecionada() {
        int selectedIndex = listaItens.getSelectedIndex();
        if (selectedIndex != -1) {
            ItemCarrinho item = listModel.getElementAt(selectedIndex);
            String novaQtdStr = JOptionPane.showInputDialog(this, "Digite a nova quantidade:", item.getQuantidade());
            try {
                int novaQtd = Integer.parseInt(novaQtdStr);
                if (novaQtd > 0) {
                    item.setQuantidade(novaQtd);
                    atualizarListaItens();
                    atualizarTotalLabel();
                } else {
                    JOptionPane.showMessageDialog(this, "Quantidade deve ser maior que zero.", "Erro", JOptionPane.ERROR_MESSAGE);
                }
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Digite um número válido.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Selecione um item para alterar.", "Aviso", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void processarCompra() {
        String endereco = enderecoTextArea.getText();
        String formaPagamento = pixRadioButton.isSelected() ? "Pix" : "Cartão de Crédito";
        double total = calcularTotalCarrinhoSingleton();

        if (endereco.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Preencha o endereço de entrega.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (Carrinho.getInstance().getItens().isEmpty()) {
            JOptionPane.showMessageDialog(this, "O carrinho está vazio.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!verificarEstoqueDisponivel()) {
            return;
        }

        int pedidoId = salvarPedido(usuarioIdCliente, endereco, formaPagamento, total, obterEmailUsuarioLogado());

        if (pedidoId > 0) {
            boolean itensSalvos = salvarItensPedido(pedidoId, Carrinho.getInstance().getItens());
            if (itensSalvos) {
                JOptionPane.showMessageDialog(this, "Compra realizada!\nPedido: " + pedidoId, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                Carrinho.getInstance().limparCarrinho();
                atualizarListaItens();
                atualizarTotalLabel();
                JOptionPane.showMessageDialog(this, "Obrigado pela sua compra!", "Finalizado", JOptionPane.INFORMATION_MESSAGE);
                System.exit(0);
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao salvar os itens do pedido.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Erro ao salvar o pedido.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean verificarEstoqueDisponivel() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            for (ItemCarrinho item : Carrinho.getInstance().getItens()) {
                String coluna = switch (item.getTamanho().toUpperCase()) {
                    case "P" -> "quantidade_p";
                    case "M" -> "quantidade_m";
                    case "G" -> "quantidade_g";
                    default -> "";
                };

                if (coluna.isEmpty()) continue;

                String sql = "SELECT " + coluna + " FROM produtos WHERE id = ?";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setInt(1, item.getProdutoId());
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        int estoque = rs.getInt(coluna);
                        if (estoque < item.getQuantidade()) {
                            JOptionPane.showMessageDialog(this,
                                    "Estoque insuficiente para o produto: " + item.getNome() +
                                            " (" + item.getTamanho() + "). Disponível: " + estoque,
                                    "Estoque Insuficiente", JOptionPane.WARNING_MESSAGE);
                            return false;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao verificar estoque: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private double calcularTotalCarrinhoSingleton() {
        return Carrinho.getInstance().getTotal();
    }

    private void atualizarListaItens() {
        listModel.clear();
        Carrinho.getInstance().getItens().forEach(listModel::addElement);
    }

    private void atualizarTotalLabel() {
        totalLabel.setText("Total: R$ " + String.format("%.2f", calcularTotalCarrinhoSingleton()));
    }

    private String obterEmailUsuarioLogado() {
        return SessaoUsuario.getInstance().getEmailUsuario();
    }

    private int salvarPedido(int usuarioId, String endereco, String formaPagamento, double total, String email) {
        int pedidoId = -1;
        String sql = "INSERT INTO pedidos (usuario_id, endereco_entrega, forma_pagamento, total, email) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, usuarioId);
            pstmt.setString(2, endereco);
            pstmt.setString(3, formaPagamento);
            pstmt.setDouble(4, total);
            pstmt.setString(5, email);
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                ResultSet rs = pstmt.getGeneratedKeys();
                if (rs.next()) {
                    pedidoId = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar pedido: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
        return pedidoId;
    }

    private boolean salvarItensPedido(int pedidoId, List<ItemCarrinho> itens) {
        boolean sucesso = true;
        String sql = "INSERT INTO itens_pedido (pedido_id, produto_id, quantidade, preco_unitario, subtotal, tamanho) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (ItemCarrinho item : itens) {
                pstmt.setInt(1, pedidoId);
                pstmt.setInt(2, item.getProdutoId());
                pstmt.setInt(3, item.getQuantidade());
                pstmt.setDouble(4, item.getValorUnitario());
                pstmt.setDouble(5, item.getSubtotal());
                pstmt.setString(6, item.getTamanho());
                pstmt.addBatch();

                atualizarEstoque(conn, item.getProdutoId(), item.getTamanho(), -item.getQuantidade());
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar itens: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            sucesso = false;
        }
        return sucesso;
    }

    private void atualizarEstoque(Connection conn, int produtoId, String tamanho, int quantidadeAlteracao) throws SQLException {
        String coluna = switch (tamanho.toUpperCase()) {
            case "P" -> "quantidade_p";
            case "M" -> "quantidade_m";
            case "G" -> "quantidade_g";
            default -> "";
        };
        if (coluna.isEmpty()) return;

        String sql = "UPDATE produtos SET " + coluna + " = " + coluna + " + ? WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, quantidadeAlteracao);
            pstmt.setInt(2, produtoId);
            pstmt.executeUpdate();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TelaCarrinho::new);
    }
}
