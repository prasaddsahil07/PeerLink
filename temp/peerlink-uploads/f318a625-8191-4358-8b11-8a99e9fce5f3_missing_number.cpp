#include<bits/stdc++.h>
using namespace std;

int main(){
    int n;
    cin>>n;
    vector<int> vec(n-1);
    vector<int> state(n, 0);
    for(int i=0; i<n-1; i++){
        cin>>vec[i];
        state[vec[i]-1] = 1;
    }
    for(int i=0; i<n; i++){
        if(state[i] == 0){
            cout<<i+1<<endl;
            break;
        }
    }
    return 0;
}