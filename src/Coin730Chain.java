import java.security.Security;
import java.util.*;
import java.util.Base64;

import com.google.gson.GsonBuilder;

public class Coin730Chain {
	public static ArrayList<Block> blockchain = new ArrayList<Block>();
	public static HashMap<String,TransactionOutput> UTXOs = new HashMap<String,TransactionOutput>();
	
	public static int difficulty = 5;
	public static float minimumTransaction = 0.1f;
	public static Wallet walletX;
	public static Wallet walletY;
	public static Wallet walletZ;
	public static Transaction genesisTransaction;
	public static Transaction secondTransaction;
	public static Transaction thirdTransaction;

	public static void main(String[] args) {	
		//add our blocks to the blockchain ArrayList:
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider()); //Setup Bouncey castle as a Security Provider
		
		//Create wallets:
		walletX = new Wallet();
		walletY = new Wallet();	
		walletZ = new Wallet();
		Wallet coinbase = new Wallet();
			                                                                      
		//create genesis transaction, which sends 200000 CUSC to walletX: 
		genesisTransaction = new Transaction(coinbase.publicKey, walletX.publicKey, CUSC_Converter.convertToUSD(200000), null);
		genesisTransaction.generateSignature(coinbase.privateKey);	 //manually sign the genesis transaction	
		genesisTransaction.transactionId = "0"; //manually set the transaction id
		genesisTransaction.outputs.add(new TransactionOutput(genesisTransaction.reciepient, genesisTransaction.value, genesisTransaction.transactionId)); //manually add the Transactions Output
		UTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0)); //its important to store our first transaction in the UTXOs list.
		
		secondTransaction = new Transaction(coinbase.publicKey, walletY.publicKey, CUSC_Converter.convertToUSD(300000), null);
		secondTransaction.generateSignature(coinbase.privateKey);
		secondTransaction.transactionId = "0";
		secondTransaction.outputs.add(new TransactionOutput(secondTransaction.reciepient, secondTransaction.value, secondTransaction.transactionId));
		UTXOs.put(secondTransaction.outputs.get(0).id, secondTransaction.outputs.get(0));
		
		thirdTransaction = new Transaction(coinbase.publicKey, walletZ.publicKey, CUSC_Converter.convertToUSD(500000), null);
		thirdTransaction.generateSignature(coinbase.privateKey);
		thirdTransaction.transactionId = "0";
		thirdTransaction.outputs.add(new TransactionOutput(thirdTransaction.reciepient, thirdTransaction.value, thirdTransaction.transactionId));
		UTXOs.put(thirdTransaction.outputs.get(0).id, thirdTransaction.outputs.get(0));
		
		System.out.println("Creating and Mining Genesis block... ");
		Block genesis = new Block("0");
		genesis.addTransaction(genesisTransaction);
		addBlock(genesis);
		
		//testing
		Block block1 = new Block(genesis.hash);
		System.out.println("\nWalletX's starting balance is: $" + walletX.getBalance());
		System.out.println("WalletY's starting balance is: $" + walletY.getBalance());
		System.out.println("WalletZ's starting balance is: $" + walletZ.getBalance());
		
		System.out.println("\nWalletX is Attempting to send funds (100) to walletY...");
		block1.addTransaction(walletX.sendFunds(walletY.publicKey, 100));            
		addBlock(block1);
		System.out.println("\nwalletX's balance is: " + walletX.getBalance());
		System.out.println("walletY's balance is: " + walletY.getBalance());
		
		Block block2 = new Block(block1.hash);
		System.out.println("\nwalletY Attempting to send funds (200) to walletZ...");
		block2.addTransaction(walletY.sendFunds(walletZ.publicKey, 200));
		addBlock(block2);
		System.out.println("\nwalletY's balance is: " + walletY.getBalance());
		System.out.println("walletZ's balance is: " + walletZ.getBalance());
		
		Block block3 = new Block(block2.hash);
		System.out.println("\nwalletZ is Attempting to send funds (400) to walletX...");
		block3.addTransaction(walletZ.sendFunds( walletX.publicKey, 400));
		System.out.println("\nwalletZ's balance is: " + walletZ.getBalance());
		System.out.println("walletX's balance is: " + walletX.getBalance());
		
		System.out.println();
		System.out.println("Final Wallet Balances: ");
		System.out.println("Wallet X balance: $"+ walletX.getBalance() + " which converts to: "+ CUSC_Converter.convertToCUSC(walletX.getBalance()) + " CUSC coins");
		System.out.println("Wallet Y balance: $"+ walletY.getBalance() + " which converts to: "+ CUSC_Converter.convertToCUSC(walletY.getBalance()) + " CUSC coins");
		System.out.println("Wallet Z balance: $"+ walletZ.getBalance() + " which converts to: "+ CUSC_Converter.convertToCUSC(walletZ.getBalance()) + " CUSC coins");
		 
		System.out.println();
		isChainValid();
		
		String blockchainJson = StringUtil.getJson(blockchain);
		System.out.println("\nThe block chain: ");
		System.out.println(blockchainJson);
		
	}
	
	public static Boolean isChainValid() {
		
		Block currentBlock; 
		Block previousBlock;
		String hashTarget = new String(new char[difficulty]).replace('\0', '0');
		HashMap<String,TransactionOutput> tempUTXOs = new HashMap<String,TransactionOutput>(); //a temporary working list of unspent transactions at a given block state.
		tempUTXOs.put(genesisTransaction.outputs.get(0).id, genesisTransaction.outputs.get(0));
		
		//loop through blockchain to check hashes:
		for(int i=1; i < blockchain.size(); i++) {
			
			currentBlock = blockchain.get(i);
			previousBlock = blockchain.get(i-1);
			//compare registered hash and calculated hash:
			if(!currentBlock.hash.equals(currentBlock.calculateHash()) ){
				System.out.println("#Current Hashes not equal");
				return false;
			}
			//compare previous hash and registered previous hash
			if(!previousBlock.hash.equals(currentBlock.previousHash) ) {
				System.out.println("#Previous Hashes not equal");
				return false;
			}
			//check if hash is solved
			if(!currentBlock.hash.substring( 0, difficulty).equals(hashTarget)) {
				System.out.println("#This block hasn't been mined");
				return false;
			}
			
			//loop thru blockchains transactions: 
			TransactionOutput tempOutput;
			for(int t=1; t <currentBlock.transactions.size(); t++) {
				Transaction currentTransaction = currentBlock.transactions.get(t);
				
				if(!currentTransaction.verifiySignature()) {
					System.out.println("#Signature on Transaction(" + t + ") is Invalid");
					return false; 
				}
				if(currentTransaction.getInputsValue() != currentTransaction.getOutputsValue()) {
					System.out.println("#Inputs are not equal to outputs on Transaction(" + t + ")");
					return false; 
				}
				
				for(TransactionInput input: currentTransaction.inputs) {	
					tempOutput = tempUTXOs.get(input.transactionOutputId);
					
					if(tempOutput == null) {
						System.out.println("#Referenced input on Transaction(" + t + ") is Missing");
						return false;
					}
					
					if(input.UTXO.value != tempOutput.value) {
						System.out.println("#Referenced input Transaction(" + t + ") value is Invalid");
						return false;
					}
					
					tempUTXOs.remove(input.transactionOutputId);
				}
				
				for(TransactionOutput output: currentTransaction.outputs) {
					tempUTXOs.put(output.id, output);
				}
				
				if( currentTransaction.outputs.get(0).reciepient != currentTransaction.reciepient) {
					System.out.println("#Transaction(" + t + ") output reciepient is not who it should be");
					return false;
				}
				if( currentTransaction.outputs.get(1).reciepient != currentTransaction.sender) {
					System.out.println("#Transaction(" + t + ") output 'change' is not sender.");
					return false;
				}
				
			}
			
		}
		System.out.println("Blockchain is valid");
		return true;                                                     
	}
	
	public static void addBlock(Block newBlock) {
		newBlock.mineBlock(difficulty);
		blockchain.add(newBlock);
	}
}
