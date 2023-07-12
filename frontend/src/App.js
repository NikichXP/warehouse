import './App.css';

function getWarehouseContent() {
    console.log("request sent")
    fetch("127.0.0.1:8080/storage/list")
        .then(res => {
            res.body.getReader().read()
                .then(r => r.value)
                .then(arr => {
                    var i, str = '';

                    for (i = 0; i < arr.length; i++) {
                        str += '%' + ('0' + arr[i].toString(16)).slice(-2);
                    }
                    str = decodeURIComponent(str);
                    console.log(str)
                })
            // return res.json()
        })
    // .then(json => console.log(json))
}

function App() {
    return (
        <div className="App">
            <h1>Hello app</h1>
            <button onClick={getWarehouseContent}>Click</button>
        </div>
    );
}

export default App;
