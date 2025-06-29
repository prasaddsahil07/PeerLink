#include<bits/stdc++.h>
using namespace std;

typedef long long ll;

const int MOD = 1e9 + 7;

void solve(int n){
    ll ans = 1LL;
    for(int i=1; i<=n; i++){
        ans = (ans * 2) % MOD; 
    }
    cout << ans << endl;
}

int main(){
    int n;
    cin >> n;
    solve(n);
    return 0;
}
