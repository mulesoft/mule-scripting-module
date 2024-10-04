// Definir las monedas de USD con sus valores
var USD = [
    { name: 'quarters', value: 25 },
    { name: 'dimes', value: 10 },
    { name: 'nickels', value: 5 },
    { name: 'pennies', value: 1 }
];

// Definir las monedas de GBP con sus valores
var GBP = [
    { name: 'two_pounds', value: 200 },
    { name: 'pounds', value: 100 },
    { name: 'fifty_pence', value: 50 },
    { name: 'twenty_pence', value: 20 },
    { name: 'ten_pence', value: 10 },
    { name: 'five_pence', value: 5 },
    { name: 'two_pence', value: 2 },
    { name: 'pennies', value: 1 }
];

// Función para hacer el cambio greedy
function change(currency, amount) {
    var result = currency.map(function(coin) {
        var count = Math.floor(amount / coin.value);
        amount = amount % coin.value;
        return count + ' ' + coin.name;
    });
    return '[' + result.join(', ') + ']';
}


// Seleccionar la moneda basada en la variable `currency` y calcular el cambio
var result;
switch (currency) {
    case "USD":
        result = change(USD, payload);
        break;
    case "GBP":
        result = change(GBP, payload);
        break;
    default:
        throw new Error("Unsupported currency: " + currency);
}

// El valor resultante será el valor final del script
result;