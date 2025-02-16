<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Investment Portfolio Risk Analysis Tool</title>
    <style>
        body {
            font-family: Arial, sans-serif;
            line-height: 1.6;
            max-width: 800px;
            margin: auto;
            padding: 20px;
        }
        h1, h2 {
            color: #2c3e50;
        }
        .section {
            margin-bottom: 20px;
        }
        .features li, .metrics li, .enhancements li {
            margin-bottom: 8px;
        }
        .checkmark {
            color: green;
            font-weight: bold;
        }
        .screenshot {
            width: 100%;
            border: 1px solid #ddd;
            border-radius: 8px;
            margin-top: 10px;
        }
        .contact a {
            color: #3498db;
            text-decoration: none;
            font-weight: bold;
        }
    </style>
</head>
<body>

    <h1>📊 Investment Portfolio Risk Analysis Tool</h1>
    <p>A <strong>Java & Spring Boot-based application</strong> for evaluating investment portfolio performance and risk exposure. This tool calculates key risk metrics and provides users with a <strong>final risk score (1 worst - 5 best)</strong> to assess their investments.</p>

    <h2>🚀 Features</h2>
    <ul class="features">
        <li><span class="checkmark">✅</span> <strong>User Accounts & Portfolios</strong> – Create accounts, build portfolios, and add assets.</li>
        <li><span class="checkmark">✅</span> <strong>Risk Metrics Calculation</strong> – Computes <em>Volatility, Sharpe Ratio, Beta, Alpha, Maximum Drawdown, and Value at Risk (VaR)</em>.</li>
        <li><span class="checkmark">✅</span> <strong>Risk Score Analysis</strong> – Generates a final <em>risk score (1-5)</em> based on calculated metrics.</li>
        <li><span class="checkmark">✅</span> <strong>Real-Time Data</strong> – Fetches financial data from the <em>AlphaVantage API</em>.</li>
        <li><span class="checkmark">✅</span> <strong>Risk-Free Rate</strong> – Uses <em>U.S. Treasury Bills (2-year maturity, daily interval)</em>.</li>
        <li><span class="checkmark">✅</span> <strong>Local Database Storage</strong> – Stores users, portfolios, assets, and risk metrics in <em>MySQL</em>.</li>
    </ul>

    <h2>🛠️ Tech Stack</h2>
    <ul>
        <li><strong>Backend:</strong> Java, Spring Boot</li>
        <li><strong>Frontend:</strong> HTML, CSS, Thymeleaf</li>
        <li><strong>Authentication:</strong> Spring Security</li>
        <li><strong>Database:</strong> MySQL</li>
        <li><strong>API Integration:</strong> AlphaVantage API (RESTful)</li>
    </ul>

    <h2>🖼️ The Application</h2>
    <p>Login Page</p>
    <img src="path-to-your-image/screenshot1.png" class="screenshot">
    <p>Dashboard</p>
    <img src="path-to-your-image/screenshot2.png" class="screenshot">
    <p>Adding Asset Form</p>
    <img src="path-to-your-image/screenshot1.png" class="screenshot">
    <p>Risk Analysis</p>
    <img src="path-to-your-image/screenshot2.png" class="screenshot">

    <h2>📈 Risk Metrics Explained</h2>
    <ul class="metrics">
        <li><strong>Volatility</strong> – Measures portfolio price fluctuations.</li>
        <li><strong>Sharpe Ratio</strong> – Risk-adjusted return measure.</li>
        <li><strong>Beta</strong> – Portfolio sensitivity to market movements.</li>
        <li><strong>Alpha</strong> – Excess return relative to benchmark.</li>
        <li><strong>Maximum Drawdown</strong> – Largest portfolio loss from peak.</li>
        <li><strong>Value at Risk (VaR)</strong> – Potential loss at a given confidence level.</li>
    </ul>

    <h2>💡 Future Enhancements</h2>
    <ul class="enhancements">
        <li>Deploy on AWS/GCP</li>
        <li>Improve UI/UX with React or Vue.js</li>
        <li>Support more financial data sources</li>
        <li>Add Monte Carlo simulations</li>
    </ul>

    <h2>📩 Contact & Contributions</h2>
    <p class="contact">Feel free to <strong>fork, contribute, or report issues!</strong>  
    <br>🔗 <a href="https://github.com/your-username">GitHub Repository</a></p>

</body>
</html>
