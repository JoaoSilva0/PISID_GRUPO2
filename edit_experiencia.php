<?php
// Conexão com a base de dados
$servername = "localhost"; 
$username = "root"; 
$password = ""; 
$dbname = "minha_base_de_dados"; 

$conn = new mysqli($servername, $username, $password, $dbname);

if ($conn->connect_error) {
    die("Falha na conexão: " . $conn->connect_error);
}

$message = ""; // Para mostrar mensagens ao usuário

// Se um ID foi fornecido via GET ou POST
if (isset($_GET['id']) || isset($_POST['id'])) {
   // $id = isset($_GET['id']) ? (int)$_GET['id'] : (int)$_POST['id'];
    $id = 1;

    if ($_SERVER['REQUEST_METHOD'] === 'POST') {
        // Se a solicitação for POST, atualize o banco de dados
        $nome = isset($_POST['nome']) ? $conn->real_escape_string($_POST['nome']) : "";
        $descricao = isset($_POST['descricao']) ? $conn->real_escape_string($_POST['descricao']) : "";
        $data = isset($_POST['data']) ? $conn->real_escape_string($_POST['data']) : "";

        $sql = "UPDATE experiencia SET nome = '$nome', Descricao = '$descricao', DataHora = '$data' WHERE IDExperiencia = $id";

        if ($conn->query($sql) === TRUE) {
            $message = "Experiência atualizada com sucesso.";
        } else {
            $message = "Erro ao atualizar experiência: " . $conn->error;
        }
    }

    // Consultar a tabela para obter os dados da experiência
    $sql = "SELECT * FROM experiencia WHERE IDExperiencia= $id";
    $result = $conn->query($sql);

    if ($result->num_rows > 0) {
        $row = $result->fetch_assoc();
    } else {
        $message = "Experiência não encontrada.";
        $row = null; // Se não encontrar nada, não exibir o formulário
    }
} else {
    $message = "ID não fornecido.";
    $row = null; // Sem ID, sem formulário
}

$conn->close();
?>

<!DOCTYPE html>
<html>
<head>
    <title>Editar Experiência</title>
</head>
<body>

<h2>Editar Experiência</h2>

<?php if (!empty($message)): ?>
    <p><?php echo $message; ?></p>
<?php endif; ?>

<?php if ($row): ?>
    <form action="edit_experiencia.php" method="post">
        <input type="hidden" name="id" value="<?php echo $id; ?>">
        <label for="nome">Nome:</label>
        <input type="text" id="nome" name="nome" value="<?php echo htmlspecialchars($row['nome']); ?>"><br>

        <label for="descricao">Descrição:</label>
        <textarea id="descricao" name="descricao"><?php echo htmlspecialchars($row['descricao']); ?></textarea><br>

        <label para="data">Data:</label>
        <input type="date" id="data" name="data" value="<?php echo $row['data']; ?>"><br>

        <input type="submit" value="Salvar">
    </form>
<?php endif; ?>

</body>
</html>
