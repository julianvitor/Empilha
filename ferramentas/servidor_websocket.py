import asyncio
import websockets

# Lista para armazenar as conexões dos clientes
connected_clients = set()

async def handle_client(websocket, path):
    # Adiciona o websocket do cliente à lista de conexões
    connected_clients.add(websocket)
    try:
        # Loop para receber mensagens do cliente
        async for message in websocket:
            print(f"Mensagem recebida de um cliente: {message}")
    finally:
        # Remove o websocket do cliente da lista de conexões quando a conexão é fechada
        connected_clients.remove(websocket)

async def send_message_to_clients(message):
    # Envia uma mensagem para todos os clientes conectados
    if connected_clients:
        await asyncio.wait([client.send(message) for client in connected_clients])

async def main():
    # Inicia o servidor WebSocket na porta 8765
    async with websockets.serve(handle_client, "localhost", 8765):
        print("Servidor WebSocket iniciado...")
        while True:
            # Aguarda a entrada do usuário para enviar uma mensagem para os clientes
            user_input = input("Digite a mensagem para enviar aos clientes (ou 'sair' para encerrar): ")
            if user_input.lower() == 'sair':
                break
            # Envia a mensagem para todos os clientes conectados
            await send_message_to_clients(user_input)

asyncio.run(main())
