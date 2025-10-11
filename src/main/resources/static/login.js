// Este é o nosso ficheiro login.js

// O código dentro disto só corre depois de a página HTML estar completamente carregada
document.addEventListener('DOMContentLoaded', function() {
    console.log("Página carregada, login.js a correr.");

    // Função de teste para o Axios
    async function testarAxios() {
        console.log("A tentar fazer uma chamada com o Axios...");
        try {
            // Usamos o Axios para fazer uma chamada GET para um endpoint público da nossa API
            const response = await axios.get('/cargos');

            // Se a chamada for bem-sucedida, mostramos os dados na consola
            console.log("Sucesso! Axios está a funcionar. Dados recebidos:", response.data);
            alert("Teste do Axios bem-sucedido! Verifique a consola (F12).");

        } catch (error) {
            // Se houver um erro, mostramos na consola
            console.error("Erro ao testar o Axios:", error);
            alert("Erro no teste do Axios. Verifique a consola (F12).");
        }
    }

    // Chama a função de teste
    testarAxios();
});