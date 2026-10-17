const fs = require('fs');
const path = require('path');
const https = require('https');

// Read key from .env file or env
let apiKey = process.env.GEMINI_API_KEY;
if (!apiKey) {
  const envPath = path.resolve(__dirname, '../../.env');
  if (fs.existsSync(envPath)) {
    const lines = fs.readFileSync(envPath, 'utf-8').split('\n');
    for (const line of lines) {
      const match = line.match(/^GEMINI_API_KEY=(.+)$/);
      if (match) {
        apiKey = match[1].trim();
        break;
      }
    }
  }
}

if (!apiKey) {
  console.error("No API key available.");
  process.exit(1);
}

function request(options, data) {
  return new Promise((resolve) => {
    const startTime = Date.now();
    const req = https.request(options, (res) => {
      let body = '';
      res.on('data', chunk => body += chunk);
      res.on('end', () => {
        const duration = Date.now() - startTime;
        let json = null;
        try {
          json = JSON.parse(body);
        } catch (e) {
          json = { raw: body };
        }
        resolve({
          statusCode: res.statusCode,
          duration,
          data: json
        });
      });
    });
    req.on('error', (err) => {
      resolve({
        statusCode: 0,
        duration: Date.now() - startTime,
        error: err.message
      });
    });
    if (data) {
      req.write(typeof data === 'string' ? data : JSON.stringify(data));
    }
    req.end();
  });
}

async function callGenerateContent(model, payload) {
  return await request({
    hostname: 'generativelanguage.googleapis.com',
    path: `/v1beta/models/${model}:generateContent`,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-goog-api-key': apiKey
    }
  }, payload);
}

async function callEmbedContent(model, text) {
  return await request({
    hostname: 'generativelanguage.googleapis.com',
    path: `/v1beta/models/${model}:embedContent`,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-goog-api-key': apiKey
    }
  }, {
    model: `models/${model}`,
    content: {
      parts: [{ text: text }]
    }
  });
}

// Candidates to test
const candidates = [
  'gemini-3.8-flash',
  'gemini-3.7-flash',
  'gemini-3.6-flash',
  'gemini-3.5-flash',
  'gemini-3.5-flash-lite',
  'gemini-3.1-flash-lite',
  'gemini-3-flash-preview',
  'gemini-2.5-flash',
  'gemini-2.5-flash-lite'
];

async function testModel(model) {
  console.log(`\n================ Testing ${model} ================`);
  const report = {
    model,
    available: false,
    vietnamese: false,
    functionCalling: false,
    structuredJson: false,
    multiTurn: false,
    toolPlusJson: false,
    latencies: [],
    p50Latency: null,
    statusCode: null,
    note: ''
  };

  // Test (a): Simple Vietnamese query
  const resA = await callGenerateContent(model, {
    contents: [{ role: 'user', parts: [{ text: 'Chào bạn, FixLink là gì? Trả lời trong 1 câu ngắn.' }] }]
  });
  report.statusCode = resA.statusCode;
  if (resA.statusCode === 200 && resA.data?.candidates?.[0]?.content?.parts?.[0]?.text) {
    report.available = true;
    report.vietnamese = true;
    console.log(`[PASS] (a) Vietnamese answer: ${resA.data.candidates[0].content.parts[0].text.substring(0, 60)}...`);
  } else {
    report.note = `Status ${resA.statusCode}: ${JSON.stringify(resA.data?.error || resA.error || '')}`;
    console.log(`[FAIL] (a) Vietnamese query failed. ${report.note}`);
    return report;
  }

  // Test (b): Function Calling with dummy tool
  const toolPayload = {
    contents: [{ role: 'user', parts: [{ text: 'Thời tiết Hà Nội hôm nay thế nào?' }] }],
    tools: [{
      functionDeclarations: [{
        name: 'get_weather',
        description: 'Lấy thông tin thời tiết tại một địa điểm',
        parameters: {
          type: 'OBJECT',
          properties: {
            location: { type: 'STRING', description: 'Tên thành phố' }
          },
          required: ['location']
        }
      }]
    }],
    toolConfig: {
      functionCallingConfig: { mode: 'AUTO' }
    }
  };
  const resB = await callGenerateContent(model, toolPayload);
  if (resB.statusCode === 200 && resB.data?.candidates?.[0]?.content?.parts?.[0]?.functionCall) {
    const fnCall = resB.data.candidates[0].content.parts[0].functionCall;
    if (fnCall.name === 'get_weather') {
      report.functionCalling = true;
      console.log(`[PASS] (b) Function Calling: successfully invoked ${fnCall.name}(${JSON.stringify(fnCall.args)})`);
    }
  } else {
    console.log(`[FAIL] (b) Function Calling: status ${resB.statusCode}, candidates: ${JSON.stringify(resB.data?.candidates?.[0]?.content?.parts)}`);
  }

  // Test (c): Structured Output JSON
  const jsonPayload = {
    contents: [{ role: 'user', parts: [{ text: 'Phân loại lỗi: "Tủ lạnh không lạnh, ngăn đá bị đông đá quá nhiều".' }] }],
    generationConfig: {
      responseMimeType: 'application/json',
      responseSchema: {
        type: 'OBJECT',
        properties: {
          category: { type: 'STRING', description: 'Tên danh mục dịch vụ' },
          severity: { type: 'STRING', enum: ['LOW', 'MEDIUM', 'HIGH'] }
        },
        required: ['category', 'severity']
      }
    }
  };
  const resC = await callGenerateContent(model, jsonPayload);
  if (resC.statusCode === 200) {
    try {
      const text = resC.data.candidates[0].content.parts[0].text;
      const parsed = JSON.parse(text);
      if (parsed.category && parsed.severity) {
        report.structuredJson = true;
        console.log(`[PASS] (c) Structured JSON: ${text.trim()}`);
      }
    } catch (e) {
      console.log(`[FAIL] (c) JSON parse error:`, e.message);
    }
  } else {
    console.log(`[FAIL] (c) Structured Output: status ${resC.statusCode}, error: ${JSON.stringify(resC.data?.error)}`);
  }

  // Test (d): Multi-turn (3 turns)
  const turnPayload = {
    contents: [
      { role: 'user', parts: [{ text: 'Tôi tên là Nam.' }] },
      { role: 'model', parts: [{ text: 'Chào anh Nam, tôi có thể giúp gì cho anh?' }] },
      { role: 'user', parts: [{ text: 'Tôi cần sửa vòi nước rò rỉ.' }] },
      { role: 'model', parts: [{ text: 'Dạ, anh Nam có thể chọn dịch vụ Ống nước.' }] },
      { role: 'user', parts: [{ text: 'Tôi tên là gì và tôi đang muốn sửa gì?' }] }
    ]
  };
  const resD = await callGenerateContent(model, turnPayload);
  if (resD.statusCode === 200) {
    const textD = resD.data?.candidates?.[0]?.content?.parts?.[0]?.text || '';
    if (textD.toLowerCase().includes('nam') && (textD.toLowerCase().includes('vòi') || textD.toLowerCase().includes('nước') || textD.toLowerCase().includes('ống'))) {
      report.multiTurn = true;
      console.log(`[PASS] (d) Multi-turn conversation: ${textD.substring(0, 70)}...`);
    } else {
      console.log(`[WARN] (d) Multi-turn answer might not recall context: ${textD}`);
    }
  } else {
    console.log(`[FAIL] (d) Multi-turn: status ${resD.statusCode}`);
  }

  // Test (f): Combination of Tool + JSON Schema in same request
  const combinedPayload = {
    contents: [{ role: 'user', parts: [{ text: 'Thời tiết Đà Nẵng thế nào?' }] }],
    tools: toolPayload.tools,
    generationConfig: {
      responseMimeType: 'application/json',
      responseSchema: {
        type: 'OBJECT',
        properties: {
          reply: { type: 'STRING' }
        }
      }
    }
  };
  const resF = await callGenerateContent(model, combinedPayload);
  if (resF.statusCode === 200) {
    report.toolPlusJson = true;
    console.log(`[INFO] (f) Can combine Tool + JSON Schema in single request: YES`);
  } else {
    report.toolPlusJson = false;
    console.log(`[INFO] (f) Can combine Tool + JSON Schema in single request: NO (Status ${resF.statusCode}: ${resF.data?.error?.message || ''})`);
  }

  // Test (e): Latency p50 over 5 calls
  console.log(`Testing latency over 5 calls for ${model}...`);
  const latencies = [];
  for (let i = 0; i < 5; i++) {
    const r = await callGenerateContent(model, {
      contents: [{ role: 'user', parts: [{ text: 'FixLink hỗ trợ những dịch vụ gì? Kể 3 dịch vụ.' }] }]
    });
    if (r.statusCode === 200) {
      latencies.push(r.duration);
    }
    // slight delay to prevent bursting
    await new Promise(res => setTimeout(res, 200));
  }
  latencies.sort((a, b) => a - b);
  report.latencies = latencies;
  if (latencies.length > 0) {
    report.p50Latency = latencies[Math.floor(latencies.length / 2)];
    console.log(`[LATENCY] Latencies: ${latencies.join(', ')} ms | p50 = ${report.p50Latency} ms`);
  }

  return report;
}

async function testEmbeddings() {
  console.log(`\n================ Testing Embedding Models ================`);
  const embedModels = ['gemini-embedding-001', 'gemini-embedding-2-preview', 'gemini-embedding-2'];
  const results = [];
  for (const m of embedModels) {
    const r = await callEmbedContent(m, 'Hệ thống hỗ trợ sửa chữa FixLink');
    const success = r.statusCode === 200 && Array.isArray(r.data?.embedding?.values);
    const dim = success ? r.data.embedding.values.length : 0;
    console.log(`Model: ${m} -> Status ${r.statusCode}, Success: ${success}, Dimension: ${dim}, Duration: ${r.duration}ms`);
    results.push({ model: m, statusCode: r.statusCode, success, dimension: dim, duration: r.duration, error: r.data?.error?.message });
  }
  return results;
}

async function main() {
  const modelReports = [];
  for (const m of candidates) {
    try {
      const rep = await testModel(m);
      modelReports.push(rep);
    } catch (e) {
      console.error(`Error testing ${m}:`, e);
    }
  }

  const embedReports = await testEmbeddings();

  const finalReport = {
    timestamp: new Date().toISOString(),
    models: modelReports,
    embeddings: embedReports
  };

  fs.writeFileSync(path.resolve(__dirname, 'probe-results.json'), JSON.stringify(finalReport, null, 2));
  console.log("\nProbe complete! Saved to scripts/chatbot/probe-results.json");
}

main().catch(console.error);
