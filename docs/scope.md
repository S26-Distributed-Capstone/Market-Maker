# Use Cases

## Submitting External Orders

External orders could be submitted into the submitted to the exchange.
Workflow diagram:

### Error Cases
- Exchange service instance goes down before handling the order
- Exchange service instance goes down after sending the fill but before providing confirmation
- Trading state service instance goes down before handling the fill

## Updating Quote

The quote is updated based on updates to the corresponding positions.
Workflow diagram:

### Error Cases
- Market maker node goes down before handling the position update
- Market maker node goes down after sending updated reservation but before sending new quote
- Reservation service instance goes down before updating reservation
- Exchange service instance goes down before updating quote

## Streaming Position Data Updates

Updated positions could be viewed as they are commited
Workflow diagram:

### Error Cases
- Connected trading state service instance goes down
