# OBD2 Car Connect - Diagnostico Veicular

Aplicacao Java/JavaFX para conectar seu PC ao carro via adaptador OBD2 (ELM327).
Suporta leitura de dados em tempo real, codigos de falha (DTC), e monitoramento da rede CAN Bus.

## Funcionalidades

- **Dashboard em Tempo Real** - RPM, velocidade, temperatura, carga do motor, etc.
- **Codigos de Falha (DTC)** - Leitura e limpeza de codigos de diagnostico
- **Monitor CAN Bus** - Sniffing de mensagens da rede CAN do veiculo
- **Terminal OBD** - Envio de comandos manuais AT e OBD2
- **Multi-protocolo** - Suporte a CAN, ISO 9141, KWP2000, J1850

## Requisitos

- Java 17+
- Maven 3.8+
- Adaptador OBD2 ELM327 (USB ou Bluetooth)

## Como Compilar

```bash
mvn clean compile
```

## Como Executar

```bash
mvn javafx:run
```

## Como Usar

1. Conecte o adaptador ELM327 ao conector OBD2 do veiculo (embaixo do painel)
2. Conecte o adaptador ao PC via USB ou Bluetooth
3. Ligue a ignicao do veiculo
4. Abra a aplicacao e selecione a porta serial
5. Clique em "Conectar"

## Protocolos Suportados

| Protocolo | Descricao |
|-----------|-----------|
| ISO 15765-4 CAN | CAN Bus 11/29 bit, 250/500 kbaud |
| ISO 9141-2 | 5 baud init |
| ISO 14230-4 KWP | Fast/slow init |
| SAE J1850 | PWM e VPW |
| SAE J1939 | CAN Bus para veiculos pesados |

## Estrutura do Projeto

```
src/main/java/com/obd2/
├── OBD2Application.java          # Ponto de entrada (JavaFX Application)
├── command/
│   ├── OBDCommand.java           # Enum de comandos OBD2 (PIDs)
│   └── OBDCommandExecutor.java   # Executor de comandos
├── connection/
│   ├── SerialConnection.java     # Conexao serial (jSerialComm)
│   └── ELM327Initializer.java    # Inicializacao do ELM327
├── gui/
│   ├── MainView.java             # View principal com abas
│   ├── ConnectionBar.java        # Barra de conexao
│   ├── DashboardTab.java         # Dashboard com gauges
│   ├── GaugeWidget.java          # Widget de gauge individual
│   ├── DTCTab.java               # Aba de codigos de falha
│   ├── CANMonitorTab.java        # Monitor CAN Bus
│   ├── TerminalTab.java          # Terminal de comandos
│   └── StatusBar.java            # Barra de status
├── model/
│   ├── OBDData.java              # Modelo de dados OBD
│   ├── CANMessage.java           # Modelo de mensagem CAN
│   └── DTCCode.java              # Modelo de codigo de falha
└── service/
    ├── OBDDataService.java       # Servico de leitura OBD2
    └── CANMonitorService.java    # Servico de monitoramento CAN
```
