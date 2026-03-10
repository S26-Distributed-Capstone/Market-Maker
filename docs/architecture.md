# Architecture

```mermaid
graph TD
    ExternalOrderPub["External Order Pub"]
    Exchange["Exchange"]
    TradingState["Trading State"]
    ExposureReservation["Exposure Reservation"]
    MarketMaker["Market Maker"]
    PositionUI["Position UI"]

    ExternalOrderPub -->|"Places buy and sell orders"| Exchange
    Exchange -->|"Sends fill data"| TradingState
    TradingState -->|"Sends position data"| MarketMaker
    TradingState -->|"Sends current position data"| PositionUI
    TradingState -->|"Updates exposures"| ExposureReservation
    ExposureReservation -->|"Warn if over exposed"| Exchange
    MarketMaker -->|"Updates quotes"| Exchange
```

